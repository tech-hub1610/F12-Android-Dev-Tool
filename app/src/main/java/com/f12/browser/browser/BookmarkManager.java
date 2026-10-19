package com.f12.browser.browser;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class BookmarkManager {
    private static final String PREF_NAME = "f12_bookmarks";
    private static final String KEY_ITEMS = "bookmark_items";
    private final SharedPreferences prefs;

    public static class BookmarkItem {
        public final String title;
        public final String url;
        public final long timestamp;

        public BookmarkItem(String title, String url, long timestamp) {
            this.title = title != null && !title.isEmpty() ? title : url;
            this.url = url;
            this.timestamp = timestamp;
        }
    }

    public BookmarkManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public List<BookmarkItem> getBookmarks() {
        List<BookmarkItem> list = new ArrayList<>();
        String raw = prefs.getString(KEY_ITEMS, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                list.add(new BookmarkItem(
                        obj.optString("title"),
                        obj.optString("url"),
                        obj.optLong("timestamp")
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    public void addBookmark(String title, String url) {
        if (url == null || url.isEmpty()) return;
        List<BookmarkItem> list = getBookmarks();
        // Check if duplicate url
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).url.equals(url)) {
                list.remove(i);
                break;
            }
        }
        list.add(0, new BookmarkItem(title, url, System.currentTimeMillis()));
        saveBookmarks(list);
    }

    public void removeBookmark(String url) {
        List<BookmarkItem> list = getBookmarks();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).url.equals(url)) {
                list.remove(i);
                saveBookmarks(list);
                break;
            }
        }
    }

    public boolean isBookmarked(String url) {
        if (url == null) return false;
        List<BookmarkItem> list = getBookmarks();
        for (BookmarkItem item : list) {
            if (item.url.equals(url)) return true;
        }
        return false;
    }

    private void saveBookmarks(List<BookmarkItem> list) {
        JSONArray arr = new JSONArray();
        for (BookmarkItem item : list) {
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
