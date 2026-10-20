package com.f12.browser.browser;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.http.SslCertificate;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import com.f12.browser.devtools.DevToolsBridge;
import com.f12.browser.devtools.DevToolsController;
import com.f12.browser.devtools.model.ConsoleMessageItem;
import com.f12.browser.devtools.model.NetworkRequestItem;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class BrowserTab {
    public static final String CHROME_MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36";

    private final String id;
    private final WebView webView;
    private String currentUrl = "";
    private String currentTitle = "New Tab";
    private int progress = 0;
    private boolean isDesktopMode = false;
    private SslCertificate sslCertificate = null;
    private static String cachedInjectedJs = null;

    private final List<ConsoleMessageItem> consoleMessages = new ArrayList<>();
    private final List<NetworkRequestItem> networkRequests = new ArrayList<>();

    @SuppressLint("SetJavaScriptEnabled")
    public BrowserTab(Context context, String id, DevToolsController devToolsController) {
        this.id = id;
        this.webView = new WebView(context);

        WebSettings settings = webView.getSettings();
        settings.setUserAgentString(CHROME_MOBILE_USER_AGENT);
        settings.setJavaScriptEnabled(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        // Enable third party cookies for Google Account authentication
        try {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);
        } catch (Exception ignored) {}

        // Add JavaScript Interface
        webView.addJavascriptInterface(new DevToolsBridge(devToolsController), "DevToolsNative");
    }

    public void injectDevToolsScript() {
        if (cachedInjectedJs == null) {
            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(webView.getContext().getAssets().open("devtools/injected.js")));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append("\n");
                }
                reader.close();
                cachedInjectedJs = sb.toString();
            } catch (Exception e) {
                cachedInjectedJs = "";
            }
        }

        if (cachedInjectedJs != null && !cachedInjectedJs.isEmpty()) {
            webView.evaluateJavascript(cachedInjectedJs, null);
        }
    }

    public String getId() { return id; }
    public WebView getWebView() { return webView; }
    public String getCurrentUrl() { return currentUrl; }
    public void setCurrentUrl(String url) { this.currentUrl = url; }
    public String getCurrentTitle() { return currentTitle; }
    public void setCurrentTitle(String title) { this.currentTitle = title; }
    public int getProgress() { return progress; }
    public void setProgress(int progress) { this.progress = progress; }
    public boolean isDesktopMode() { return isDesktopMode; }

    public void setDesktopMode(boolean desktop) {
        this.isDesktopMode = desktop;
        if (desktop) {
            webView.getSettings().setUserAgentString(SettingsManager.DESKTOP_USER_AGENT);
            webView.getSettings().setUseWideViewPort(true);
        } else {
            webView.getSettings().setUserAgentString(CHROME_MOBILE_USER_AGENT);
            webView.getSettings().setUseWideViewPort(true);
        }
    }

    public SslCertificate getSslCertificate() { return sslCertificate; }
    public void setSslCertificate(SslCertificate cert) { this.sslCertificate = cert; }

    public List<ConsoleMessageItem> getConsoleMessages() { return consoleMessages; }
    public void addConsoleMessage(ConsoleMessageItem item) {
        consoleMessages.add(item);
    }
    public void clearConsoleMessages() {
        consoleMessages.clear();
    }

    public List<NetworkRequestItem> getNetworkRequests() { return networkRequests; }
    public void addNetworkRequest(NetworkRequestItem item) {
        networkRequests.add(item);
    }
    public void clearNetworkRequests() {
        networkRequests.clear();
    }

    public void destroy() {
        try {
            webView.stopLoading();
            webView.clearHistory();
            webView.removeAllViews();
            webView.destroy();
        } catch (Exception ignored) {}
    }
}
