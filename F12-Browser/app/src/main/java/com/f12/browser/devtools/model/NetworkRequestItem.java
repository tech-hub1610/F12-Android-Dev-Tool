package com.f12.browser.devtools.model;

import org.json.JSONObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class NetworkRequestItem {
    private final String url;
    private final String method;
    private final int statusCode;
    private final String statusText;
    private final long duration;
    private final long startTime;
    private final String resourceType; // fetch, xhr, doc, script, stylesheet, image, other
    private final Map<String, String> requestHeaders;
    private final String requestBody;
    private final Map<String, String> responseHeaders;
    private final String responseBody;

    public NetworkRequestItem(String url, String method, int statusCode, String statusText,
                              long duration, long startTime, String resourceType,
                              Map<String, String> requestHeaders, String requestBody,
                              Map<String, String> responseHeaders, String responseBody) {
        this.url = url;
        this.method = method;
        this.statusCode = statusCode;
        this.statusText = statusText;
        this.duration = duration;
        this.startTime = startTime;
        this.resourceType = resourceType;
        this.requestHeaders = requestHeaders != null ? requestHeaders : new HashMap<>();
        this.requestBody = requestBody != null ? requestBody : "";
        this.responseHeaders = responseHeaders != null ? responseHeaders : new HashMap<>();
        this.responseBody = responseBody != null ? responseBody : "";
    }

    public static NetworkRequestItem fromJson(JSONObject json) {
        String url = json.optString("url", "");
        String method = json.optString("method", "GET");
        int status = json.optInt("status", 200);
        String statusText = json.optString("statusText", "");
        long duration = json.optLong("duration", 0);
        long startTime = json.optLong("startTime", System.currentTimeMillis());
        String type = json.optString("type", "fetch");

        Map<String, String> reqHeaders = new HashMap<>();
        JSONObject reqH = json.optJSONObject("requestHeaders");
        if (reqH != null) {
            Iterator<String> keys = reqH.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                reqHeaders.put(k, reqH.optString(k));
            }
        }

        String reqBody = json.optString("requestBody", "");

        Map<String, String> respHeaders = new HashMap<>();
        JSONObject respH = json.optJSONObject("responseHeaders");
        if (respH != null) {
            Iterator<String> keys = respH.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                respHeaders.put(k, respH.optString(k));
            }
        }

        String respBody = json.optString("responseBody", "");

        return new NetworkRequestItem(url, method, status, statusText, duration, startTime, type,
                reqHeaders, reqBody, respHeaders, respBody);
    }

    public String getUrl() { return url; }
    public String getMethod() { return method; }
    public int getStatusCode() { return statusCode; }
    public String getStatusText() { return statusText; }
    public long getDuration() { return duration; }
    public long getStartTime() { return startTime; }
    public String getResourceType() { return resourceType; }
    public Map<String, String> getRequestHeaders() { return requestHeaders; }
    public String getRequestBody() { return requestBody; }
    public Map<String, String> getResponseHeaders() { return responseHeaders; }
    public String getResponseBody() { return responseBody; }

    public String getFileName() {
        if (url == null || url.isEmpty()) return "unknown";
        int lastSlash = url.lastIndexOf('/');
        int question = url.indexOf('?');
        if (lastSlash != -1) {
            if (question > lastSlash) {
                return url.substring(lastSlash + 1, question);
            }
            return url.substring(lastSlash + 1);
        }
        return url;
    }
}
