package com.f12.browser;

import com.f12.browser.devtools.model.AuditItem;
import com.f12.browser.devtools.model.ConsoleMessageItem;
import com.f12.browser.devtools.model.DomNode;
import com.f12.browser.devtools.model.NetworkRequestItem;
import com.f12.browser.util.FormatUtils;
import com.f12.browser.util.UrlUtils;
import org.json.JSONObject;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class BrowserCoreUnitTest {

    @Test
    public void testSmartUrlProcessing() {
        assertEquals("https://example.com", UrlUtils.smartUrl("example.com", "https://www.google.com/search?q="));
        assertEquals("https://example.com/path", UrlUtils.smartUrl("https://example.com/path", "https://www.google.com/search?q="));
        assertEquals("http://localhost:3000", UrlUtils.smartUrl("localhost:3000", "https://www.google.com/search?q="));
        assertTrue(UrlUtils.smartUrl("search query test", "https://www.google.com/search?q=").startsWith("https://www.google.com/search?q="));
    }

    @Test
    public void testExtractDomain() {
        assertEquals("example.com", UrlUtils.extractDomain("https://example.com/some/path?param=1"));
        assertEquals("localhost", UrlUtils.extractDomain("http://localhost:8080"));
        assertEquals("Local Page", UrlUtils.extractDomain("file:///android_asset/home/index.html"));
    }

    @Test
    public void testFormatDuration() {
        assertEquals("250ms", FormatUtils.formatDuration(250));
        assertEquals("1.50s", FormatUtils.formatDuration(1500));
    }

    @Test
    public void testFormatBytes() {
        assertEquals("500 B", FormatUtils.formatBytes(500));
        assertEquals("1.0 KB", FormatUtils.formatBytes(1024));
    }

    @Test
    public void testConsoleMessageItem() {
        ConsoleMessageItem item = new ConsoleMessageItem(
                ConsoleMessageItem.Level.ERROR,
                "Uncaught reference error",
                "app.js",
                42,
                1000000L
        );
        assertEquals(ConsoleMessageItem.Level.ERROR, item.getLevel());
        assertEquals("Uncaught reference error", item.getMessage());
        assertEquals("app.js", item.getSource());
        assertEquals(42, item.getLineNumber());
    }

    @Test
    public void testNetworkRequestItem() {
        Map<String, String> reqH = new HashMap<>();
        reqH.put("Accept", "application/json");
        Map<String, String> respH = new HashMap<>();
        respH.put("Content-Type", "application/json");

        NetworkRequestItem item = new NetworkRequestItem(
                "https://api.example.com/v1/users",
                "GET",
                200,
                "OK",
                120,
                System.currentTimeMillis(),
                "fetch",
                reqH,
                "",
                respH,
                "{\"users\":[]}"
        );

        assertEquals("https://api.example.com/v1/users", item.getUrl());
        assertEquals("GET", item.getMethod());
        assertEquals(200, item.getStatusCode());
        assertEquals("users", item.getFileName());
        assertEquals("fetch", item.getResourceType());
        assertEquals("{\"users\":[]}", item.getResponseBody());
    }

    @Test
    public void testDomNodeOpeningTag() {
        Map<String, String> attrs = new HashMap<>();
        attrs.put("data-test", "true");
        DomNode node = new DomNode(1, "node_1", "button", "submit-btn", "btn primary", attrs, 0, "", null);
        String tag = node.getFormattedOpeningTag();
        assertTrue(tag.contains("<button"));
        assertTrue(tag.contains("id=\"submit-btn\""));
        assertTrue(tag.contains("class=\"btn primary\""));
        assertTrue(tag.contains("data-test=\"true\""));
    }

    @Test
    public void testAuditItemCreation() {
        AuditItem item = new AuditItem(
                "SEO",
                "Document Title",
                1.0,
                AuditItem.Severity.PASS,
                "Found title",
                "None"
        );
        assertEquals("SEO", item.getCategory());
        assertEquals(AuditItem.Severity.PASS, item.getSeverity());
        assertEquals(1.0, item.getScore(), 0.001);
    }

    @Test
    public void testParseChromeBookmarks() {
        String sampleHtml = "<!DOCTYPE NETSCAPE-Bookmark-file-1><DL><p>" +
                "<DT><A HREF=\"https://news.google.com\" ADD_DATE=\"1700000000\">Google News</A>" +
                "<DT><A HREF=\"https://github.com\">GitHub</A>" +
                "</DL><p>";
        java.util.List<com.f12.browser.browser.BookmarkManager.BookmarkItem> items =
                com.f12.browser.google.GoogleDataManager.parseChromeBookmarks(sampleHtml);
        assertEquals(2, items.size());
        assertEquals("Google News", items.get(0).title);
        assertEquals("https://news.google.com", items.get(0).url);
        assertEquals("GitHub", items.get(1).title);
        assertEquals("https://github.com", items.get(1).url);
    }

    @Test
    public void testExportChromeBookmarks() {
        java.util.List<com.f12.browser.browser.BookmarkManager.BookmarkItem> list = new java.util.ArrayList<>();
        list.add(new com.f12.browser.browser.BookmarkManager.BookmarkItem("MDN Web Docs", "https://developer.mozilla.org", 1700000000000L));
        String html = com.f12.browser.google.GoogleDataManager.exportToChromeBookmarksHtml(list);
        assertTrue(html.contains("<!DOCTYPE NETSCAPE-Bookmark-file-1>"));
        assertTrue(html.contains("HREF=\"https://developer.mozilla.org\""));
        assertTrue(html.contains("MDN Web Docs"));
    }

    @Test
    public void testParseTakeoutHistory() {
        String sampleJson = "{\"Browser History\": [" +
                "{\"title\": \"Search Result\", \"url\": \"https://www.google.com/search?q=android\", \"time_usec\": 1700000000000000}" +
                "]}";
        java.util.List<com.f12.browser.browser.HistoryManager.HistoryItem> list =
                com.f12.browser.google.GoogleDataManager.parseTakeoutHistory(sampleJson);
        assertEquals(1, list.size());
        assertEquals("Search Result", list.get(0).title);
        assertEquals("https://www.google.com/search?q=android", list.get(0).url);
    }
}
