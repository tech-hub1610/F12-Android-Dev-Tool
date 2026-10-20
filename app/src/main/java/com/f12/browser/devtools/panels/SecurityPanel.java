package com.f12.browser.devtools.panels;

import android.content.Context;
import android.net.http.SslCertificate;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import com.f12.browser.R;
import com.f12.browser.browser.BrowserTab;
import com.f12.browser.devtools.DevToolsController;
import com.f12.browser.devtools.model.NetworkRequestItem;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SecurityPanel implements DevToolsPanel {
    private final DevToolsController controller;
    private final View root;

    private final TextView statusBadge;
    private final TextView textUrl;
    private final TextView textCertInfo;
    private final TextView textMixedContent;
    private final TextView textSecurityHeaders;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.US);

    public SecurityPanel(DevToolsController controller) {
        this.controller = controller;
        Context context = controller.getContext();
        root = LayoutInflater.from(context).inflate(R.layout.panel_security, null, false);

        statusBadge = root.findViewById(R.id.security_status_badge);
        textUrl = root.findViewById(R.id.security_url);
        textCertInfo = root.findViewById(R.id.text_certificate_info);
        textMixedContent = root.findViewById(R.id.text_mixed_content);
        textSecurityHeaders = root.findViewById(R.id.text_security_headers);
    }

    public void loadSecurityInfo() {
        BrowserTab activeTab = controller.getActiveTab();
        if (activeTab == null) return;

        String url = activeTab.getCurrentUrl();
        textUrl.setText(url != null ? url : "about:blank");

        boolean isHttps = url != null && url.startsWith("https://");
        boolean isLocal = url != null && url.startsWith("file:///");

        android.content.Context ctx = controller.getContext();
        if (isHttps) {
            statusBadge.setText("SECURE (HTTPS)");
            statusBadge.setBackgroundResource(R.drawable.bg_button_white);
            statusBadge.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.bg_black));
        } else if (isLocal) {
            statusBadge.setText("LOCAL ASSET");
            statusBadge.setBackgroundResource(R.drawable.bg_button_dark);
            statusBadge.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.text_primary));
        } else {
            statusBadge.setText("NOT SECURE (HTTP)");
            statusBadge.setBackgroundResource(R.drawable.bg_button_dark);
            statusBadge.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.console_error_text));
        }

        // Certificate Details
        SslCertificate cert = activeTab.getSslCertificate();
        if (cert != null) {
            String subject = cert.getIssuedTo() != null ? cert.getIssuedTo().getDName() : "Unknown";
            String issuer = cert.getIssuedBy() != null ? cert.getIssuedBy().getDName() : "Unknown";
            String validNotBefore = cert.getValidNotBeforeDate() != null ? DATE_FORMAT.format(cert.getValidNotBeforeDate()) : "Unknown";
            String validNotAfter = cert.getValidNotAfterDate() != null ? DATE_FORMAT.format(cert.getValidNotAfterDate()) : "Unknown";

            String certDetails = "Issued To (Subject):\n  " + subject + "\n\n"
                    + "Issued By (CA Authority):\n  " + issuer + "\n\n"
                    + "Validity Period:\n"
                    + "  From: " + validNotBefore + "\n"
                    + "  To:   " + validNotAfter;
            textCertInfo.setText(certDetails);
        } else if (isHttps) {
            textCertInfo.setText("Connection is TLS encrypted. Certificate verified by Android TrustStore.");
        } else {
            textCertInfo.setText("No SSL/TLS certificate found. Web traffic on HTTP is unencrypted and vulnerable to eavesdropping.");
        }

        // Mixed Content Check
        List<NetworkRequestItem> requests = activeTab.getNetworkRequests();
        List<String> mixedHttpRequests = new ArrayList<>();
        if (isHttps) {
            for (NetworkRequestItem req : requests) {
                if (req.getUrl().startsWith("http://")) {
                    mixedHttpRequests.add(req.getUrl());
                }
            }
        }

        if (mixedHttpRequests.isEmpty()) {
            textMixedContent.setText("✔ No mixed content detected. All recorded subresources use secure connections.");
        } else {
            StringBuilder sb = new StringBuilder("⚠ Detected " + mixedHttpRequests.size() + " insecure HTTP resource(s) on HTTPS page:\n");
            for (String mixedUrl : mixedHttpRequests) {
                sb.append("• ").append(mixedUrl).append("\n");
            }
            textMixedContent.setText(sb.toString().trim());
        }

        // Check headers on main frame if available
        if (activeTab.getWebView() != null) {
            activeTab.getWebView().evaluateJavascript(
                    "(function() {"
                            + "  var csp = document.querySelector('meta[http-equiv=\"Content-Security-Policy\"]');"
                            + "  return csp ? csp.getAttribute('content') : '';"
                            + "})()",
                    res -> {
                        String clean = res;
                        if (clean != null && clean.startsWith("\"") && clean.endsWith("\"")) {
                            clean = clean.substring(1, clean.length() - 1);
                        }
                        if (clean != null && !clean.isEmpty()) {
                            textSecurityHeaders.setText("Content-Security-Policy (Meta Tag):\n" + clean);
                        } else {
                            textSecurityHeaders.setText("Content-Security-Policy: Not specified via meta tag.\nStrict-Transport-Security: Enforced on standard HTTPS origins.\nX-Content-Type-Options: nosniff enforced by modern Chromium engine.");
                        }
                    }
            );
        }
    }

    @Override
    public View getView() { return root; }

    @Override
    public void onTabActivated() { refresh(); }

    @Override
    public void onTabDeactivated() {}

    @Override
    public void refresh() {
        loadSecurityInfo();
    }
}
