package com.f12.browser.browser;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.f12.browser.devtools.DevToolsController;
import com.f12.browser.devtools.model.NetworkRequestItem;
import java.util.HashMap;
import android.content.Intent;
import android.net.Uri;
import com.f12.browser.util.UrlUtils;
import java.util.Map;

public class BrowserWebViewClient extends WebViewClient {
    public interface PageListener {
        void onPageStarted(String url);
        void onPageFinished(String url, String title);
        void onPageError(String failingUrl, String description);
        void onDashboardRequested();
    }

    private final BrowserTab tab;
    private final DevToolsController devToolsController;
    private final PageListener pageListener;

    public BrowserWebViewClient(BrowserTab tab, DevToolsController devToolsController, PageListener pageListener) {
        this.tab = tab;
        this.devToolsController = devToolsController;
        this.pageListener = pageListener;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        if (request != null && request.getUrl() != null) {
            return handleUrlOverride(view, request.getUrl().toString());
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return handleUrlOverride(view, url);
    }

    private boolean handleUrlOverride(WebView view, String url) {
        if (url == null || url.isEmpty()) return false;

        // Catch dashboard URL and route natively
        if (url.equalsIgnoreCase(UrlUtils.DASHBOARD_URL)) {
            if (pageListener != null) {
                pageListener.onDashboardRequested();
            }
            return true;
        }

        // Catch non-web URI schemes to avoid ERR_UNKNOWN_URL_SCHEME
        if (!url.startsWith("http://") && !url.startsWith("https://")
                && !url.startsWith("file://") && !url.startsWith("about:") && !url.startsWith("data:")) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                view.getContext().startActivity(intent);
            } catch (Exception ignored) {}
            return true;
        }
        return false;
    }

    @Override
    public void onPageStarted(WebView view, String url, Bitmap favicon) {
        super.onPageStarted(view, url, favicon);

        if (url == null || url.equals("about:blank") || url.equalsIgnoreCase(UrlUtils.DASHBOARD_URL)) {
            if (pageListener != null) {
                pageListener.onPageStarted(url);
            }
            return;
        }

        tab.setCurrentUrl(url);
        tab.setSslCertificate(view.getCertificate());

        // Clear logs if not preserving
        if (devToolsController != null && !devToolsController.getSettingsManager().isPreserveLogs()) {
            tab.clearConsoleMessages();
            tab.clearNetworkRequests();
        }

        // Early injection
        tab.injectDevToolsScript();

        if (pageListener != null) {
            pageListener.onPageStarted(url);
        }
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);

        if (tab.isClearHistoryPending()) {
            tab.setClearHistoryPending(false);
            view.clearHistory();
            tab.setSessionStartIndex(0);
        } else if (tab.getSessionStartIndex() < 0) {
            android.webkit.WebBackForwardList history = view.copyBackForwardList();
            tab.setSessionStartIndex(history != null ? history.getCurrentIndex() : 0);
        }

        if (url == null || url.equals("about:blank") || url.equalsIgnoreCase(UrlUtils.DASHBOARD_URL)) {
            if (pageListener != null) {
                pageListener.onPageFinished(url, "");
            }
            return;
        }

        tab.setCurrentUrl(url);
        tab.setCurrentTitle(view.getTitle());
        tab.setSslCertificate(view.getCertificate());

        // Ensure devtools script injected
        tab.injectDevToolsScript();

        if (pageListener != null) {
            pageListener.onPageFinished(url, view.getTitle());
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
        super.onReceivedError(view, errorCode, description, failingUrl);
        if (failingUrl != null && (failingUrl.equalsIgnoreCase(UrlUtils.DASHBOARD_URL) || failingUrl.equals("about:blank"))) {
            return;
        }
        if (pageListener != null) {
            pageListener.onPageError(failingUrl, description);
        }
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if (request != null && request.getUrl() != null) {
            String url = request.getUrl().toString();
            String method = request.getMethod();
            Map<String, String> headers = request.getRequestHeaders();

            // Derive resource type
            String type = "other";
            String lowerUrl = url.toLowerCase();
            if (request.isForMainFrame()) type = "document";
            else if (lowerUrl.endsWith(".js") || lowerUrl.contains(".js?")) type = "script";
            else if (lowerUrl.endsWith(".css") || lowerUrl.contains(".css?")) type = "stylesheet";
            else if (lowerUrl.matches(".*\\.(png|jpg|jpeg|gif|webp|svg|ico).*")) type = "image";
            else if (lowerUrl.matches(".*\\.(mp4|webm|mp3|ogg|wav).*")) type = "media";
            else if (lowerUrl.matches(".*\\.(woff|woff2|ttf|otf).*")) type = "font";

            NetworkRequestItem item = new NetworkRequestItem(
                    url,
                    method,
                    200,
                    "OK",
                    0,
                    System.currentTimeMillis(),
                    type,
                    headers != null ? headers : new HashMap<>(),
                    "",
                    new HashMap<>(),
                    ""
            );

            // Record to DevTools
            if (devToolsController != null) {
                devToolsController.onSubresourceIntercepted(tab, item);
            }
        }
        return super.shouldInterceptRequest(view, request);
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        Context context = view.getContext();
        String message = "SSL Certificate Warning!\n";
        if (error != null) {
            switch (error.getPrimaryError()) {
                case SslError.SSL_EXPIRED:
                    message += "The certificate has expired.";
                    break;
                case SslError.SSL_IDMISMATCH:
                    message += "Hostname mismatch.";
                    break;
                case SslError.SSL_UNTRUSTED:
                    message += "Certificate authority is not trusted.";
                    break;
                case SslError.SSL_NOTYETVALID:
                    message += "The certificate is not yet valid.";
                    break;
                default:
                    message += "SSL error occurred (" + error.getPrimaryError() + ").";
                    break;
            }
            if (error.getUrl() != null) {
                message += "\nURL: " + error.getUrl();
            }
        }

        // Never automatically proceed SSL errors! Present clear security choice to user.
        new AlertDialog.Builder(context)
                .setTitle("SSL Security Warning")
                .setMessage(message + "\n\nDo you want to proceed at your own risk?")
                .setPositiveButton("Proceed (Unsafe)", (dialog, which) -> handler.proceed())
                .setNegativeButton("Cancel (Recommended)", (dialog, which) -> handler.cancel())
                .setCancelable(false)
                .show();
    }
}
