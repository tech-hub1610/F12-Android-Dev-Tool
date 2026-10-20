package com.f12.browser.browser;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    private static final String PREF_NAME = "f12_settings";
    private static final String KEY_HOMEPAGE = "pref_homepage";
    private static final String KEY_SEARCH_ENGINE = "pref_search_engine";
    private static final String KEY_DESKTOP_MODE = "pref_desktop_mode";
    private static final String KEY_PRESERVE_LOGS = "pref_preserve_logs";

    public static final String SEARCH_GOOGLE = "https://www.google.com/search?q=";
    public static final String SEARCH_DUCKDUCKGO = "https://duckduckgo.com/?q=";
    public static final String SEARCH_BING = "https://www.bing.com/search?q=";

    public static final String DESKTOP_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    private final SharedPreferences prefs;

    public SettingsManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public String getHomepage() {
        return prefs.getString(KEY_HOMEPAGE, com.f12.browser.util.UrlUtils.DASHBOARD_URL);
    }

    public void setHomepage(String homepage) {
        prefs.edit().putString(KEY_HOMEPAGE, homepage).apply();
    }

    public String getSearchEngine() {
        return prefs.getString(KEY_SEARCH_ENGINE, SEARCH_GOOGLE);
    }

    public void setSearchEngine(String engine) {
        prefs.edit().putString(KEY_SEARCH_ENGINE, engine).apply();
    }

    public boolean isDesktopMode() {
        return prefs.getBoolean(KEY_DESKTOP_MODE, false);
    }

    public void setDesktopMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_DESKTOP_MODE, enabled).apply();
    }

    public boolean isPreserveLogs() {
        return prefs.getBoolean(KEY_PRESERVE_LOGS, false);
    }

    public void setPreserveLogs(boolean preserve) {
        prefs.edit().putBoolean(KEY_PRESERVE_LOGS, preserve).apply();
    }
}
