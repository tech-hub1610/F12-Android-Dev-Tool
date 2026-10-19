package com.f12.browser.util;

import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class FormatUtils {
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    public static String formatTime(long timestamp) {
        return TIME_FORMAT.format(new Date(timestamp));
    }

    public static String formatDuration(long millis) {
        if (millis < 1000) return millis + "ms";
        return String.format(Locale.US, "%.2fs", millis / 1000.0);
    }

    public static String formatJsonPretty(String rawJson) {
        if (rawJson == null || rawJson.trim().isEmpty()) return "";
        try {
            String trimmed = rawJson.trim();
            if (trimmed.startsWith("{")) {
                JSONObject obj = new JSONObject(trimmed);
                return obj.toString(2);
            } else if (trimmed.startsWith("[")) {
                JSONArray arr = new JSONArray(trimmed);
                return arr.toString(2);
            }
        } catch (Exception ignored) {}
        return rawJson;
    }

    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format(Locale.US, "%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }
}
