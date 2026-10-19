package com.f12.browser.util;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UrlUtils {
    public static final String DEFAULT_SEARCH_ENGINE_URL = "https://www.google.com/search?q=";
    public static final String DASHBOARD_URL = "f12://dashboard";
    public static final String HOME_URL = DASHBOARD_URL;

    private static final Pattern DOMAIN_PATTERN = Pattern.compile("^[a-zA-Z0-9][-a-zA-Z0-9]*(\\.[a-zA-Z0-9][-a-zA-Z0-9]*)+(:\\d+)?(/.*)?$");
    private static final Pattern EXTRACT_HOST_PATTERN = Pattern.compile("^(?:https?://)?([^/:?#]+)");

    public static String smartUrl(String query, String searchEngineBaseUrl) {
        if (query == null) return DASHBOARD_URL;
        String trimmed = query.trim();
        if (trimmed.isEmpty()) return DASHBOARD_URL;

        if (trimmed.equalsIgnoreCase("about:blank") || trimmed.startsWith("file:///") || trimmed.equalsIgnoreCase(DASHBOARD_URL)) {
            return trimmed;
        }

        // If it starts with http:// or https://
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }

        // Check if looks like localhost:port or ip:port
        if (trimmed.matches("^(localhost|127\\.0\\.0\\.1|10\\.0\\.2\\.2)(:\\d+)?(/.*)?$")) {
            return "http://" + trimmed;
        }

        // Check if valid web domain format (e.g. example.com or example.com/path)
        if (!trimmed.contains(" ") && (DOMAIN_PATTERN.matcher(trimmed).matches() || trimmed.contains("."))) {
            return "https://" + trimmed;
        }

        // Otherwise treat as search query
        try {
            String encoded = URLEncoder.encode(trimmed, StandardCharsets.UTF_8.name());
            String engine = searchEngineBaseUrl != null && !searchEngineBaseUrl.isEmpty() 
                    ? searchEngineBaseUrl : DEFAULT_SEARCH_ENGINE_URL;
            return engine + encoded;
        } catch (Exception e) {
            return DEFAULT_SEARCH_ENGINE_URL + trimmed;
        }
    }

    public static String extractDomain(String url) {
        if (url == null || url.isEmpty() || url.equals(DASHBOARD_URL)) return "Dashboard";
        if (url.startsWith("file:///")) return "Local Page";
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host != null && !host.isEmpty()) return host;
        } catch (Exception ignored) {}

        try {
            Matcher m = EXTRACT_HOST_PATTERN.matcher(url);
            if (m.find()) {
                return m.group(1);
            }
        } catch (Exception ignored) {}

        return url;
    }
}
