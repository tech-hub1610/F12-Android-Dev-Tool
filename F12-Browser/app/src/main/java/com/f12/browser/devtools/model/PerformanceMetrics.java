package com.f12.browser.devtools.model;

import org.json.JSONObject;

public class PerformanceMetrics {
    public long dns;
    public long tcp;
    public long ttfb;
    public long domContentLoaded;
    public long loadComplete;

    public long firstPaint;
    public long firstContentfulPaint;

    public double usedJsHeapMb;
    public double totalJsHeapMb;

    public int domCount;
    public int scriptsCount;
    public int stylesCount;
    public int imagesCount;

    public static PerformanceMetrics fromJson(JSONObject json) {
        PerformanceMetrics m = new PerformanceMetrics();
        if (json == null) return m;

        JSONObject nav = json.optJSONObject("navigation");
        if (nav != null) {
            m.dns = nav.optLong("dns", 0);
            m.tcp = nav.optLong("tcp", 0);
            m.ttfb = nav.optLong("ttfb", 0);
            m.domContentLoaded = nav.optLong("domContentLoaded", 0);
            m.loadComplete = nav.optLong("loadComplete", 0);
        }

        JSONObject paints = json.optJSONObject("paints");
        if (paints != null) {
            m.firstPaint = paints.optLong("first-paint", 0);
            m.firstContentfulPaint = paints.optLong("first-contentful-paint", 0);
        }

        JSONObject mem = json.optJSONObject("memory");
        if (mem != null) {
            m.usedJsHeapMb = mem.optDouble("usedJSHeap", 0);
            m.totalJsHeapMb = mem.optDouble("totalJSHeap", 0);
        }

        m.domCount = json.optInt("domCount", 0);
        m.scriptsCount = json.optInt("scriptsCount", 0);
        m.stylesCount = json.optInt("stylesCount", 0);
        m.imagesCount = json.optInt("imagesCount", 0);

        return m;
    }
}
