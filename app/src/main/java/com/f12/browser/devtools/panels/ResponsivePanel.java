package com.f12.browser.devtools.panels;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.f12.browser.R;
import com.f12.browser.browser.BrowserTab;
import com.f12.browser.devtools.DevToolsController;

public class ResponsivePanel implements DevToolsPanel {
    private final DevToolsController controller;
    private final View root;

    private final EditText inputWidth;
    private final EditText inputHeight;
    private final TextView textMediaQueries;

    private int currentSimulatedWidth = 0;
    private int currentSimulatedHeight = 0;

    public ResponsivePanel(DevToolsController controller) {
        this.controller = controller;
        Context context = controller.getContext();
        root = LayoutInflater.from(context).inflate(R.layout.panel_responsive, null, false);

        inputWidth = root.findViewById(R.id.input_width);
        inputHeight = root.findViewById(R.id.input_height);
        textMediaQueries = root.findViewById(R.id.text_media_queries);

        root.findViewById(R.id.btn_preset_iphone).setOnClickListener(v -> applyDimensions(393, 852));
        root.findViewById(R.id.btn_preset_pixel).setOnClickListener(v -> applyDimensions(412, 915));
        root.findViewById(R.id.btn_preset_ipad).setOnClickListener(v -> applyDimensions(768, 1024));
        root.findViewById(R.id.btn_preset_desktop).setOnClickListener(v -> applyDimensions(1920, 1080));
        root.findViewById(R.id.btn_preset_reset).setOnClickListener(v -> resetViewport());

        root.findViewById(R.id.btn_apply_dimensions).setOnClickListener(v -> {
            try {
                int w = Integer.parseInt(inputWidth.getText().toString().trim());
                int h = Integer.parseInt(inputHeight.getText().toString().trim());
                applyDimensions(w, h);
            } catch (Exception e) {
                Toast.makeText(context, "Please enter valid numeric dimensions", Toast.LENGTH_SHORT).show();
            }
        });

        root.findViewById(R.id.btn_toggle_orientation).setOnClickListener(v -> {
            if (currentSimulatedWidth > 0 && currentSimulatedHeight > 0) {
                int temp = currentSimulatedWidth;
                applyDimensions(currentSimulatedHeight, temp);
            } else {
                Toast.makeText(context, "Select a preset or apply dimensions first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyDimensions(int width, int height) {
        currentSimulatedWidth = width;
        currentSimulatedHeight = height;
        inputWidth.setText(String.valueOf(width));
        inputHeight.setText(String.valueOf(height));

        BrowserTab activeTab = controller.getActiveTab();
        if (activeTab != null && activeTab.getWebView() != null) {
            String js = "window.__F12__ && window.__F12__.setViewportMeta(" + width + ", " + height + ")";
            activeTab.getWebView().evaluateJavascript(js, res -> {
                loadMediaQueryDiagnostics();
                Toast.makeText(controller.getContext(), "Simulated viewport set to " + width + " × " + height, Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void resetViewport() {
        currentSimulatedWidth = 0;
        currentSimulatedHeight = 0;
        inputWidth.setText("");
        inputHeight.setText("");

        BrowserTab activeTab = controller.getActiveTab();
        if (activeTab != null && activeTab.getWebView() != null) {
            activeTab.getWebView().evaluateJavascript("window.__F12__ && window.__F12__.setViewportMeta(0, 0)", res -> {
                loadMediaQueryDiagnostics();
                Toast.makeText(controller.getContext(), "Reset to default device viewport", Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void loadMediaQueryDiagnostics() {
        BrowserTab activeTab = controller.getActiveTab();
        if (activeTab == null || activeTab.getWebView() == null) return;

        String queryScript = "(function() {"
                + "  var queries = ["
                + "    '(max-width: 480px)',"
                + "    '(max-width: 600px)',"
                + "    '(max-width: 768px)',"
                + "    '(min-width: 769px)',"
                + "    '(min-width: 1024px)',"
                + "    '(min-width: 1200px)',"
                + "    '(orientation: portrait)',"
                + "    '(orientation: landscape)'"
                + "  ];"
                + "  var results = [];"
                + "  results.push('Window Inner: ' + window.innerWidth + ' × ' + window.innerHeight);"
                + "  results.push('Screen:       ' + screen.width + ' × ' + screen.height);"
                + "  results.push('Pixel Ratio:  ' + window.devicePixelRatio + 'x\\n');"
                + "  queries.forEach(function(q) {"
                + "    var matches = window.matchMedia(q).matches;"
                + "    results.push((matches ? '✔ MATCH: ' : '  FALSE: ') + q);"
                + "  });"
                + "  return results.join('\\n');"
                + "})()";

        activeTab.getWebView().evaluateJavascript(queryScript, res -> {
            if (res == null || res.equals("null")) return;
            String clean = res;
            if (clean.startsWith("\"") && clean.endsWith("\"") && clean.length() >= 2) {
                clean = clean.substring(1, clean.length() - 1)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"");
            }
            textMediaQueries.setText(clean);
        });
    }

    @Override
    public View getView() { return root; }

    @Override
    public void onTabActivated() { refresh(); }

    @Override
    public void onTabDeactivated() {}

    @Override
    public void refresh() {
        loadMediaQueryDiagnostics();
    }
}
