package com.f12.browser.google;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.webkit.CookieManager;
import com.f12.browser.browser.BookmarkManager;
import com.f12.browser.browser.HistoryManager;
import com.f12.browser.browser.SettingsManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GoogleDataManager {

    public static final String GOOGLE_SIGN_IN_URL = "https://accounts.google.com/ServiceLogin";
    public static final String GOOGLE_MANAGE_ACCOUNT_URL = "https://myaccount.google.com/";
    public static final String GOOGLE_TAKEOUT_URL = "https://takeout.google.com/settings/takeout/custom/chrome";
    public static final String GOOGLE_DRIVE_URL = "https://drive.google.com/";
    public static final String GOOGLE_GMAIL_URL = "https://mail.google.com/";
    public static final String GOOGLE_YOUTUBE_URL = "https://www.youtube.com/";
    public static final String GOOGLE_KEEP_URL = "https://keep.google.com/";

    /**
     * Checks if a Google account session is active in WebView cookies.
     */
    public static boolean isGoogleSignedIn() {
        try {
            String cookies = CookieManager.getInstance().getCookie("https://accounts.google.com");
            if (cookies != null) {
                return cookies.contains("OSID=") || cookies.contains("SID=") || cookies.contains("SAPISID=") || cookies.contains("SSID=");
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Reads entire text content from a content Uri or stream.
     */
    public static String readContentFromUri(ContentResolver resolver, Uri uri) throws Exception {
        InputStream is = resolver.openInputStream(uri);
        if (is == null) throw new IllegalArgumentException("Cannot open file stream");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        is.close();
        return sb.toString();
    }

    // =========================================================================
    // Chrome Netscape Bookmarks HTML Parser & Exporter
    // =========================================================================

    /**
     * Parses standard Google Chrome / Netscape bookmarks HTML.
     */
    public static List<BookmarkManager.BookmarkItem> parseChromeBookmarks(String htmlContent) {
        List<BookmarkManager.BookmarkItem> result = new ArrayList<>();
        if (htmlContent == null || htmlContent.isEmpty()) return result;

        // Matches <A HREF="..." ADD_DATE="...">Title</A>
        Pattern pattern = Pattern.compile("<a\\s+[^>]*?href=[\"']([^\"']+)[\"'][^>]*>(.*?)</a>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(htmlContent);

        Pattern datePattern = Pattern.compile("add_date=[\"'](\\d+)[\"']", Pattern.CASE_INSENSITIVE);

        while (matcher.find()) {
            String url = matcher.group(1);
            String title = matcher.group(2);
            if (url == null || url.trim().isEmpty() || url.startsWith("place:") || url.startsWith("javascript:")) {
                continue;
            }

            // Clean title HTML entities & tags
            if (title != null) {
                title = title.replaceAll("<[^>]*>", "").trim();
                title = title.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'");
            }
            if (title == null || title.isEmpty()) {
                title = url;
            }

            long timestamp = System.currentTimeMillis();
            try {
                String fullTag = matcher.group(0);
                Matcher dateMatcher = datePattern.matcher(fullTag);
                if (dateMatcher.find()) {
                    long sec = Long.parseLong(dateMatcher.group(1));
                    timestamp = sec * 1000L;
                }
            } catch (Exception ignored) {}

            result.add(new BookmarkManager.BookmarkItem(title, url, timestamp));
        }
        return result;
    }

    /**
     * Generates a standard Netscape Bookmarks HTML string compatible with Google Chrome and all desktop/mobile browsers.
     */
    public static String exportToChromeBookmarksHtml(List<BookmarkManager.BookmarkItem> bookmarks) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE NETSCAPE-Bookmark-file-1>\n");
        sb.append("<!-- This is an automatically generated file by F12 Browser. -->\n");
        sb.append("<META HTTP-EQUIV=\"Content-Type\" CONTENT=\"text/html; charset=UTF-8\">\n");
        sb.append("<TITLE>Bookmarks</TITLE>\n");
        sb.append("<H1>Bookmarks</H1>\n");
        sb.append("<DL><p>\n");
        sb.append("    <DT><H3 ADD_DATE=\"").append(System.currentTimeMillis() / 1000)
          .append("\" LAST_MODIFIED=\"").append(System.currentTimeMillis() / 1000)
          .append("\" PERSONAL_TOOLBAR_FOLDER=\"true\">F12 Bookmarks</H3>\n");
        sb.append("    <DL><p>\n");

        for (BookmarkManager.BookmarkItem item : bookmarks) {
            long addDate = item.timestamp > 0 ? (item.timestamp / 1000) : (System.currentTimeMillis() / 1000);
            String safeTitle = item.title.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
            String safeUrl = item.url.replace("&", "&amp;").replace("\"", "%22");
            sb.append("        <DT><A HREF=\"").append(safeUrl)
              .append("\" ADD_DATE=\"").append(addDate).append("\">")
              .append(safeTitle).append("</A>\n");
        }

        sb.append("    </DL><p>\n");
        sb.append("</DL><p>\n");
        return sb.toString();
    }

    // =========================================================================
    // Google Takeout Chrome History Parser & Exporter
    // =========================================================================

    /**
     * Parses Google Takeout BrowserHistory.json or a standard JSON history array.
     */
    public static List<HistoryManager.HistoryItem> parseTakeoutHistory(String jsonContent) {
        List<HistoryManager.HistoryItem> result = new ArrayList<>();
        if (jsonContent == null || jsonContent.isEmpty()) return result;

        try {
            JSONArray array = null;
            if (jsonContent.trim().startsWith("[")) {
                array = new JSONArray(jsonContent);
            } else {
                JSONObject root = new JSONObject(jsonContent);
                if (root.has("Browser History")) {
                    array = root.getJSONArray("Browser History");
                } else if (root.has("history")) {
                    array = root.getJSONArray("history");
                }
            }

            if (array != null) {
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = array.getJSONObject(i);
                    String url = obj.optString("url");
                    if (url == null || url.isEmpty() || url.startsWith("chrome://")) continue;

                    String title = obj.optString("title", url);
                    long timestamp = System.currentTimeMillis();

                    if (obj.has("time_usec")) {
                        // Google Takeout uses microseconds
                        long usec = obj.optLong("time_usec");
                        timestamp = usec / 1000L;
                    } else if (obj.has("timestamp")) {
                        timestamp = obj.optLong("timestamp");
                    }

                    result.add(new HistoryManager.HistoryItem(title, url, timestamp));
                }
            }
        } catch (Throwable ignored) {
            // Fallback regex parser (also ensures desktop JVM unit tests without android stubs pass)
            Pattern itemPattern = Pattern.compile("\\{[^}]*?\"url\"\\s*:\\s*\"([^\"]+)\"[^}]*?\\}");
            Matcher m = itemPattern.matcher(jsonContent);
            Pattern titlePat = Pattern.compile("\"title\"\\s*:\\s*\"([^\"]+)\"");
            Pattern timePat = Pattern.compile("\"(?:time_usec|timestamp)\"\\s*:\\s*(\\d+)");

            while (m.find()) {
                String block = m.group(0);
                String url = m.group(1);
                if (url.startsWith("chrome://")) continue;
                String title = url;
                long timestamp = System.currentTimeMillis();

                Matcher tm = titlePat.matcher(block);
                if (tm.find()) title = tm.group(1);

                Matcher tim = timePat.matcher(block);
                if (tim.find()) {
                    try {
                        long val = Long.parseLong(tim.group(1));
                        timestamp = val > 10000000000000L ? (val / 1000L) : val;
                    } catch (Exception ignoredNum) {}
                }
                result.add(new HistoryManager.HistoryItem(title, url, timestamp));
            }
        }
        return result;
    }

    /**
     * Exports history in Google Takeout compatible JSON format.
     */
    public static String exportToGoogleTakeoutJson(List<HistoryManager.HistoryItem> historyList) {
        try {
            JSONObject root = new JSONObject();
            JSONArray array = new JSONArray();

            for (HistoryManager.HistoryItem item : historyList) {
                JSONObject obj = new JSONObject();
                obj.put("title", item.title);
                obj.put("url", item.url);
                obj.put("time_usec", item.timestamp * 1000L);
                array.put(obj);
            }

            root.put("Browser History", array);
            return root.toString(2);
        } catch (Exception e) {
            return "{\"Browser History\": []}";
        }
    }

    // =========================================================================
    // Full Data Backup & Restore (JSON)
    // =========================================================================

    public static String exportFullDataBackup(BookmarkManager bm, HistoryManager hm, SettingsManager sm) {
        try {
            JSONObject root = new JSONObject();
            root.put("app", "F12 Browser");
            root.put("version", "1.1.0");
            root.put("export_date", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));

            // Bookmarks
            JSONArray bmArray = new JSONArray();
            for (BookmarkManager.BookmarkItem item : bm.getBookmarks()) {
                JSONObject b = new JSONObject();
                b.put("title", item.title);
                b.put("url", item.url);
                b.put("timestamp", item.timestamp);
                bmArray.put(b);
            }
            root.put("bookmarks", bmArray);

            // History
            JSONArray histArray = new JSONArray();
            for (HistoryManager.HistoryItem item : hm.getHistory()) {
                JSONObject h = new JSONObject();
                h.put("title", item.title);
                h.put("url", item.url);
                h.put("timestamp", item.timestamp);
                histArray.put(h);
            }
            root.put("history", histArray);

            // Settings
            JSONObject sObj = new JSONObject();
            sObj.put("homepage", sm.getHomepage());
            sObj.put("search_engine", sm.getSearchEngine());
            sObj.put("preserve_logs", sm.isPreserveLogs());
            root.put("settings", sObj);

            return root.toString(2);
        } catch (Exception e) {
            return "{}";
        }
    }

    // =========================================================================
    // File Export Saver
    // =========================================================================

    public static File saveFileToDownloads(String fileName, String content) throws Exception {
        File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File targetFile = new File(dir, fileName);
        FileOutputStream fos = new FileOutputStream(targetFile);
        fos.write(content.getBytes(StandardCharsets.UTF_8));
        fos.flush();
        fos.close();
        return targetFile;
    }
}
