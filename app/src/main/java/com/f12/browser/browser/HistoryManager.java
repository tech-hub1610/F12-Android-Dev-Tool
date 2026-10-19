package com.f12.browser.browser;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class HistoryManager {
    private static final String PREF_NAME = "f12_history";
    private static final String KEY_ITEMS = "history_items";
    private static final int MAX_HISTORY = 200;
    private final SharedPreferences prefs;

    public static class HistoryItem {
        public final String title;
        public final String url;
        public final long timestamp;

        public HistoryItem(String title, String url, long timestamp) {
            this.title = title != null && !title.isEmpty() ? title : url;
            this.url = url;
            this.timestamp = timestamp;
        }
    }

    public HistoryManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public List<HistoryItem> getHistory() {
        List<HistoryItem> list = new ArrayList<>();
        String raw = prefs.getString(KEY_ITEMS, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new HistoryItem(
                        obj.optString("title"),
                        obj.optString("url"),
                        obj.optLong("timestamp")
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    public void addHistory(String title, String url) {
        if (url == null || url.isEmpty() || url.startsWith("file:///android_asset/")) return;
        List<HistoryItem> list = getHistory();
        // Remove if identical URL was recent
        for (int i = 0; i < Math.min(list.size(), 10); i++) {
            if (list.get(i).url.equals(url)) {
                list.remove(i);
                break;
            }
        }
        list.add(0, new HistoryItem(title, url, System.currentTimeMillis()));
        if (list.size() > MAX_HISTORY) {
            list = list.subList(0, MAX_HISTORY);
        }
        saveHistory(list);
    }

    public void clearHistory() {
        prefs.edit().remove(KEY_ITEMS).apply();
    }

    private void saveHistory(List<HistoryItem> list) {
        JSONArray arr = new JSONArray();
        for (HistoryItem item : list) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("title", item.title);
                obj.put("url", item.url);
                obj.put("timestamp", item.timestamp);
                arr.put(obj);
            } catch (Exception ignored) {}
        }
        prefs.edit().putString(KEY_ITEMS, arr.toString()).apply();
    }
}
