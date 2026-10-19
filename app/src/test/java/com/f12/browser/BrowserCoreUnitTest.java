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
}
