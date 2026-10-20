package com.f12.browser.browser;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SpeedDialManager {
    private static final String PREF_NAME = "f12_speed_dial";
    private static final String KEY_ITEMS = "speed_dial_items";
    private final SharedPreferences prefs;

    public static class SpeedDialItem {
        public final String id;
        public final String title;
        public final String url;
        public final String shortCode;

        public SpeedDialItem(String id, String title, String url, String shortCode) {
            this.id = id != null ? id : UUID.randomUUID().toString();
            this.title = title != null && !title.isEmpty() ? title : "Shortcut";
            this.url = url != null ? url : "";
            this.shortCode = shortCode != null && !shortCode.isEmpty() ? shortCode : deriveShortCode(this.title, this.url);
        }

        private static String deriveShortCode(String title, String url) {
            if (title != null && !title.trim().isEmpty()) {
                String clean = title.trim();
                if (clean.length() <= 3) return clean.toUpperCase();
                String[] words = clean.split("\\s+");
                if (words.length >= 2) {
                    return ("" + words[0].charAt(0) + words[1].charAt(0)).toUpperCase();
                }
                return clean.substring(0, Math.min(3, clean.length())).toUpperCase();
            }
            if (url != null && !url.trim().isEmpty()) {
                try {
                    android.net.Uri uri = android.net.Uri.parse(url);
                    String host = uri.getHost();
                    if (host != null && host.length() > 0) {
                        return host.substring(0, Math.min(3, host.length())).toUpperCase();
                    }
                } catch (Exception ignored) {}
            }
            return "★";
        }
    }

    public SpeedDialManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        // Initialize default presets if empty
        if (!prefs.contains(KEY_ITEMS)) {
            resetToDefaults();
        }
    }

    public List<SpeedDialItem> getSpeedDials() {
        List<SpeedDialItem> list = new ArrayList<>();
        String raw = prefs.getString(KEY_ITEMS, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new SpeedDialItem(
                        obj.optString("id"),
                        obj.optString("title"),
                        obj.optString("url"),
                        obj.optString("shortCode")
                ));
            }
        } catch (Exception ignored) {}

        if (list.isEmpty() && !prefs.contains(KEY_ITEMS)) {
            list = getDefaultPresets();
            saveSpeedDials(list);
        }
        return list;
    }

    public void addSpeedDial(String title, String url, String shortCode) {
        if (url == null || url.trim().isEmpty()) return;
        List<SpeedDialItem> list = getSpeedDials();
        list.add(new SpeedDialItem(UUID.randomUUID().toString(), title.trim(), url.trim(), shortCode != null ? shortCode.trim() : ""));
        saveSpeedDials(list);
    }

    public void updateSpeedDial(String id, String title, String url, String shortCode) {
        if (id == null || url == null || url.trim().isEmpty()) return;
        List<SpeedDialItem> list = getSpeedDials();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(id)) {
                list.set(i, new SpeedDialItem(id, title.trim(), url.trim(), shortCode != null ? shortCode.trim() : ""));
                saveSpeedDials(list);
                break;
            }
        }
    }

    public void deleteSpeedDial(String id) {
        if (id == null) return;
        List<SpeedDialItem> list = getSpeedDials();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id.equals(id)) {
                list.remove(i);
                saveSpeedDials(list);
                break;
            }
        }
    }

    public void resetToDefaults() {
        saveSpeedDials(getDefaultPresets());
    }

    private List<SpeedDialItem> getDefaultPresets() {
        List<SpeedDialItem> presets = new ArrayList<>();
        presets.add(new SpeedDialItem(UUID.randomUUID().toString(), "Google", "https://www.google.com", "G"));
        presets.add(new SpeedDialItem(UUID.randomUUID().toString(), "GitHub", "https://github.com", "GH"));
        presets.add(new SpeedDialItem(UUID.randomUUID().toString(), "MDN Docs", "https://developer.mozilla.org", "MDN"));
        presets.add(new SpeedDialItem(UUID.randomUUID().toString(), "StackOverflow", "https://stackoverflow.com", "SO"));
        presets.add(new SpeedDialItem(UUID.randomUUID().toString(), "Can I Use", "https://caniuse.com", "CIU"));
        presets.add(new SpeedDialItem(UUID.randomUUID().toString(), "Localhost", "http://localhost:3000", ":3000"));
        presets.add(new SpeedDialItem(UUID.randomUUID().toString(), "Emulator", "http://10.0.2.2:8080", ":8080"));
        presets.add(new SpeedDialItem(UUID.randomUUID().toString(), "Sandbox", "file:///android_asset/home/index.html", "</>"));
        return presets;
    }

    private void saveSpeedDials(List<SpeedDialItem> items) {
        JSONArray arr = new JSONArray();
        for (SpeedDialItem item : items) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("id", item.id);
                obj.put("title", item.title);
                obj.put("url", item.url);
                obj.put("shortCode", item.shortCode);
                arr.put(obj);
            } catch (Exception ignored) {}
        }
        prefs.edit().putString(KEY_ITEMS, arr.toString()).apply();
    }
}
