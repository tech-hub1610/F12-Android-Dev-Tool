package com.f12.browser.browser;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsManager {
    private static final String PREF_NAME = "f12_settings";
    private static final String KEY_HOMEPAGE = "pref_homepage";
    private static final String KEY_SEARCH_ENGINE = "pref_search_engine";
    private static final String KEY_DESKTOP_MODE = "pref_desktop_mode";
    private static final String KEY_PRESERVE_LOGS = "pref_preserve_logs";
    private static final String KEY_THEME_MODE = "pref_theme_mode";
    private static final String KEY_WALLPAPER_ENABLED = "pref_wallpaper_enabled";
    private static final String KEY_WALLPAPER_URL = "pref_wallpaper_url";

    public static final String SEARCH_GOOGLE = "https://www.google.com/search?q=";
    public static final String SEARCH_DUCKDUCKGO = "https://duckduckgo.com/?q=";
    public static final String SEARCH_BING = "https://www.bing.com/search?q=";

    public static final String THEME_DARK = "dark";
    public static final String THEME_LIGHT = "light";
    public static final String THEME_SYSTEM = "system";

    // Fast, high quality CDN wallpapers
    public static final String DEFAULT_WALLPAPER_URL =
            "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=1080&q=80";

    public static final String[] CDN_WALLPAPER_PRESETS = new String[] {
            "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?auto=format&fit=crop&w=1080&q=80",
            "https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=1080&q=80",
            "https://images.unsplash.com/photo-1518770660439-4636190af475?auto=format&fit=crop&w=1080&q=80",
            "https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?auto=format&fit=crop&w=1080&q=80"
    };

    public static final String[] CDN_WALLPAPER_NAMES = new String[] {
            "Abstract Flow (Default)",
            "Deep Tech Cyber",
            "Minimalist Circuit",
            "Matrix Code"
    };

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

    public String getThemeMode() {
        return prefs.getString(KEY_THEME_MODE, THEME_DARK);
    }

    public void setThemeMode(String themeMode) {
        prefs.edit().putString(KEY_THEME_MODE, themeMode).apply();
    }

    public boolean isWallpaperEnabled() {
        return prefs.getBoolean(KEY_WALLPAPER_ENABLED, true);
    }

    public void setWallpaperEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_WALLPAPER_ENABLED, enabled).apply();
    }

    public String getWallpaperUrl() {
        return prefs.getString(KEY_WALLPAPER_URL, DEFAULT_WALLPAPER_URL);
    }

    public void setWallpaperUrl(String url) {
        prefs.edit().putString(KEY_WALLPAPER_URL, url).apply();
    }
}
