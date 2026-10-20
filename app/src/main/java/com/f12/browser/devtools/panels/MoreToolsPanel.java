package com.f12.browser.devtools.panels;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.f12.browser.R;
import com.f12.browser.browser.BrowserTab;
import com.f12.browser.devtools.DevToolsController;
import com.f12.browser.devtools.model.ConsoleMessageItem;
import com.f12.browser.devtools.model.NetworkRequestItem;
import com.f12.browser.util.FormatUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MoreToolsPanel implements DevToolsPanel {
    private final DevToolsController controller;
    private final View root;

    private final Button btnUaSelector;
    private final TextView textCurrentUa;
    private final EditText inputLat;
    private final EditText inputLng;

    private static final String UA_CHROME_DESKTOP = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final String UA_SAFARI_IOS = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";
    private static final String UA_FIREFOX = "Mozilla/5.0 (Android; Mobile; rv:120.0) Gecko/120.0 Firefox/120.0";

    public MoreToolsPanel(DevToolsController controller) {
        this.controller = controller;
        Context context = controller.getContext();
        root = LayoutInflater.from(context).inflate(R.layout.panel_more_tools, null, false);

        btnUaSelector = root.findViewById(R.id.btn_ua_selector);
        textCurrentUa = root.findViewById(R.id.text_current_ua);
        inputLat = root.findViewById(R.id.input_mock_lat);
        inputLng = root.findViewById(R.id.input_mock_lng);

        btnUaSelector.setOnClickListener(v -> showUaDialog());
        root.findViewById(R.id.btn_apply_geo).setOnClickListener(v -> applyMockGeolocation());

        root.findViewById(R.id.btn_tool_json).setOnClickListener(v -> showJsonDialog());
        root.findViewById(R.id.btn_tool_url).setOnClickListener(v -> showUrlCodecDialog());
        root.findViewById(R.id.btn_tool_base64).setOnClickListener(v -> showBase64Dialog());
        root.findViewById(R.id.btn_tool_regex).setOnClickListener(v -> showRegexDialog());

        root.findViewById(R.id.btn_export_all_diagnostics).setOnClickListener(v -> exportAllDiagnostics());
    }

    private void showUaDialog() {
        String[] options = {"Default Android Mobile", "Desktop Chrome (Linux)", "Safari (iOS)", "Firefox Mobile", "Custom User-Agent..."};
        new AlertDialog.Builder(controller.getContext())
                .setTitle("Select User-Agent")
                .setItems(options, (dialog, which) -> {
                    BrowserTab activeTab = controller.getActiveTab();
                    if (activeTab == null || activeTab.getWebView() == null) return;

                    if (which == 0) {
                        activeTab.getWebView().getSettings().setUserAgentString(null);
                        btnUaSelector.setText("Default Android Mobile");
                    } else if (which == 1) {
                        activeTab.getWebView().getSettings().setUserAgentString(UA_CHROME_DESKTOP);
                        btnUaSelector.setText("Desktop Chrome");
                    } else if (which == 2) {
                        activeTab.getWebView().getSettings().setUserAgentString(UA_SAFARI_IOS);
                        btnUaSelector.setText("Safari iOS");
                    } else if (which == 3) {
                        activeTab.getWebView().getSettings().setUserAgentString(UA_FIREFOX);
                        btnUaSelector.setText("Firefox Mobile");
                    } else if (which == 4) {
                        showCustomUaDialog();
                        return;
                    }
                    updateUaDisplay();
                    activeTab.getWebView().reload();
                })
                .show();
    }

    private void showCustomUaDialog() {
        Context context = controller.getContext();
        EditText edit = new EditText(context);
        edit.setText(textCurrentUa.getText().toString());
        new AlertDialog.Builder(context)
                .setTitle("Custom User-Agent")
                .setView(edit)
                .setPositiveButton("Apply & Reload", (d, w) -> {
                    String custom = edit.getText().toString().trim();
                    BrowserTab activeTab = controller.getActiveTab();
                    if (activeTab != null && activeTab.getWebView() != null) {
                        activeTab.getWebView().getSettings().setUserAgentString(custom);
                        btnUaSelector.setText("Custom User-Agent");
                        updateUaDisplay();
                        activeTab.getWebView().reload();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void updateUaDisplay() {
        BrowserTab activeTab = controller.getActiveTab();
        if (activeTab != null && activeTab.getWebView() != null) {
            String ua = activeTab.getWebView().getSettings().getUserAgentString();
            textCurrentUa.setText(ua != null ? ua : "(Default WebView UA)");
        }
    }

    private void applyMockGeolocation() {
        String latStr = inputLat.getText().toString().trim();
        String lngStr = inputLng.getText().toString().trim();
        if (latStr.isEmpty() || lngStr.isEmpty()) {
            Toast.makeText(controller.getContext(), "Enter latitude and longitude", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            double lat = Double.parseDouble(latStr);
            double lng = Double.parseDouble(lngStr);
            String mockScript = "navigator.geolocation.getCurrentPosition = function(success) {"
                    + "  success({"
                    + "    coords: {"
                    + "      latitude: " + lat + ","
                    + "      longitude: " + lng + ","
                    + "      accuracy: 10,"
                    + "      altitude: null,"
                    + "      altitudeAccuracy: null,"
                    + "      heading: null,"
                    + "      speed: null"
                    + "    },"
                    + "    timestamp: Date.now()"
                    + "  });"
                    + "};";

            BrowserTab activeTab = controller.getActiveTab();
            if (activeTab != null && activeTab.getWebView() != null) {
                activeTab.getWebView().evaluateJavascript(mockScript, res -> {
                    Toast.makeText(controller.getContext(), "Mock geolocation applied (" + lat + ", " + lng + ")", Toast.LENGTH_SHORT).show();
                });
            }
        } catch (Exception e) {
            Toast.makeText(controller.getContext(), "Invalid numbers", Toast.LENGTH_SHORT).show();
        }
    }

    private void showJsonDialog() {
        Context context = controller.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_tool_json, null, false);
        EditText input = view.findViewById(R.id.json_input);
        TextView output = view.findViewById(R.id.json_output);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("JSON Formatter & Validator")
                .setView(view)
                .setNegativeButton("Close", null)
                .create();

        view.findViewById(R.id.btn_json_format).setOnClickListener(v -> {
            String raw = input.getText().toString().trim();
            try {
                if (raw.startsWith("{")) {
                    JSONObject obj = new JSONObject(raw);
                    output.setText(obj.toString(2));
                } else if (raw.startsWith("[")) {
                    JSONArray arr = new JSONArray(raw);
                    output.setText(arr.toString(2));
                } else {
                    output.setText("Error: Not a valid JSON object or array");
                }
            } catch (Exception e) {
                output.setText("JSON Syntax Error: " + e.getMessage());
            }
        });

        view.findViewById(R.id.btn_json_minify).setOnClickListener(v -> {
            String raw = input.getText().toString().trim();
            try {
                if (raw.startsWith("{")) {
                    JSONObject obj = new JSONObject(raw);
                    output.setText(obj.toString());
                } else if (raw.startsWith("[")) {
                    JSONArray arr = new JSONArray(raw);
                    output.setText(arr.toString());
                } else {
                    output.setText("Error: Not a valid JSON object or array");
                }
            } catch (Exception e) {
                output.setText("JSON Syntax Error: " + e.getMessage());
            }
        });

        dialog.show();
    }

    private void showUrlCodecDialog() {
        Context context = controller.getContext();
        int textPrimary = androidx.core.content.ContextCompat.getColor(context, R.color.text_primary);
        int textTertiary = androidx.core.content.ContextCompat.getColor(context, R.color.text_tertiary);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 16);

        EditText input = new EditText(context);
        input.setHint("Input URL or text...");
        input.setTextColor(textPrimary);
        input.setHintTextColor(textTertiary);
        layout.addView(input);

        LinearLayout btns = new LinearLayout(context);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setPadding(0, 16, 0, 16);

        Button btnEnc = new Button(context);
        btnEnc.setText("ENCODE");
        btnEnc.setBackgroundResource(R.drawable.bg_button_dark);
        btnEnc.setTextColor(textPrimary);
        btns.addView(btnEnc);

        Button btnDec = new Button(context);
        btnDec.setText("DECODE");
        btnDec.setBackgroundResource(R.drawable.bg_button_dark);
        btnDec.setTextColor(textPrimary);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = 16;
        btnDec.setLayoutParams(lp);
        btns.addView(btnDec);

        layout.addView(btns);

        TextView output = new TextView(context);
        output.setTextColor(textPrimary);
        output.setTextIsSelectable(true);
        output.setBackgroundResource(R.drawable.bg_code_box);
        output.setPadding(16, 16, 16, 16);
        layout.addView(output);

        btnEnc.setOnClickListener(v -> {
            try {
                output.setText(URLEncoder.encode(input.getText().toString(), StandardCharsets.UTF_8.name()));
            } catch (Exception e) {
                output.setText("Error: " + e.getMessage());
            }
        });

        btnDec.setOnClickListener(v -> {
            try {
                output.setText(URLDecoder.decode(input.getText().toString(), StandardCharsets.UTF_8.name()));
            } catch (Exception e) {
                output.setText("Error: " + e.getMessage());
            }
        });

        new AlertDialog.Builder(context)
                .setTitle("URL Encoder / Decoder")
                .setView(layout)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showBase64Dialog() {
        Context context = controller.getContext();
        int textPrimary = androidx.core.content.ContextCompat.getColor(context, R.color.text_primary);
        int textTertiary = androidx.core.content.ContextCompat.getColor(context, R.color.text_tertiary);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 16);

        EditText input = new EditText(context);
        input.setHint("Input text...");
        input.setTextColor(textPrimary);
        input.setHintTextColor(textTertiary);
        layout.addView(input);

        LinearLayout btns = new LinearLayout(context);
        btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setPadding(0, 16, 0, 16);

        Button btnEnc = new Button(context);
        btnEnc.setText("ENCODE");
        btnEnc.setBackgroundResource(R.drawable.bg_button_dark);
        btnEnc.setTextColor(textPrimary);
        btns.addView(btnEnc);

        Button btnDec = new Button(context);
        btnDec.setText("DECODE");
        btnDec.setBackgroundResource(R.drawable.bg_button_dark);
        btnDec.setTextColor(textPrimary);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = 16;
        btnDec.setLayoutParams(lp);
        btns.addView(btnDec);

        layout.addView(btns);

        TextView output = new TextView(context);
        output.setTextColor(textPrimary);
        output.setTextIsSelectable(true);
        output.setBackgroundResource(R.drawable.bg_code_box);
        output.setPadding(16, 16, 16, 16);
        layout.addView(output);

        btnEnc.setOnClickListener(v -> {
            try {
                output.setText(Base64.encodeToString(input.getText().toString().getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP));
            } catch (Exception e) {
                output.setText("Error: " + e.getMessage());
            }
        });

        btnDec.setOnClickListener(v -> {
            try {
                byte[] decoded = Base64.decode(input.getText().toString().trim(), Base64.DEFAULT);
                output.setText(new String(decoded, StandardCharsets.UTF_8));
            } catch (Exception e) {
                output.setText("Base64 Decode Error: " + e.getMessage());
            }
        });

        new AlertDialog.Builder(context)
                .setTitle("Base64 Codec")
                .setView(layout)
                .setNegativeButton("Close", null)
                .show();
    }

    private void showRegexDialog() {
        Context context = controller.getContext();
        int textPrimary = androidx.core.content.ContextCompat.getColor(context, R.color.text_primary);
        int textTertiary = androidx.core.content.ContextCompat.getColor(context, R.color.text_tertiary);
        int bgBlack = androidx.core.content.ContextCompat.getColor(context, R.color.bg_black);

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 16);

        EditText patternInput = new EditText(context);
        patternInput.setHint("Regex Pattern (e.g. \\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b)");
        patternInput.setTextColor(textPrimary);
        patternInput.setHintTextColor(textTertiary);
        layout.addView(patternInput);

        EditText testInput = new EditText(context);
        testInput.setHint("Test String");
        testInput.setTextColor(textPrimary);
        testInput.setHintTextColor(textTertiary);
        layout.addView(testInput);

        Button btnTest = new Button(context);
        btnTest.setText("TEST REGEX");
        btnTest.setBackgroundResource(R.drawable.bg_button_white);
        btnTest.setTextColor(bgBlack);
        layout.addView(btnTest);

        TextView output = new TextView(context);
        output.setTextColor(textPrimary);
        output.setTextIsSelectable(true);
        output.setBackgroundResource(R.drawable.bg_code_box);
        output.setPadding(16, 16, 16, 16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 16;
        output.setLayoutParams(lp);
        layout.addView(output);

        btnTest.setOnClickListener(v -> {
            String pStr = patternInput.getText().toString();
            String tStr = testInput.getText().toString();
            try {
                Pattern pattern = Pattern.compile(pStr);
                Matcher matcher = pattern.matcher(tStr);
                StringBuilder sb = new StringBuilder();
                int matchCount = 0;
                while (matcher.find()) {
                    matchCount++;
                    sb.append("Match #").append(matchCount).append(": \"")
                            .append(matcher.group()).append("\" (at ")
                            .append(matcher.start()).append("..").append(matcher.end()).append(")\n");
                }
                if (matchCount == 0) {
                    output.setText("No matches found.");
                } else {
                    output.setText("Found " + matchCount + " match(es):\n" + sb.toString());
                }
            } catch (Exception e) {
                output.setText("Regex Pattern Error: " + e.getMessage());
            }
        });

        new AlertDialog.Builder(context)
                .setTitle("Regular Expression Tester")
                .setView(layout)
                .setNegativeButton("Close", null)
                .show();
    }

    private void exportAllDiagnostics() {
        BrowserTab activeTab = controller.getActiveTab();
        if (activeTab == null) return;

        try {
            JSONObject bundle = new JSONObject();
            bundle.put("app", "F12 Browser");
            bundle.put("url", activeTab.getCurrentUrl());
            bundle.put("title", activeTab.getCurrentTitle());
            bundle.put("timestamp", System.currentTimeMillis());

            // Console Logs
            JSONArray logs = new JSONArray();
            for (ConsoleMessageItem item : activeTab.getConsoleMessages()) {
                JSONObject l = new JSONObject();
                l.put("level", item.getLevel().name());
                l.put("message", item.getMessage());
                l.put("source", item.getSource());
                l.put("line", item.getLineNumber());
                l.put("time", item.getTimestamp());
                logs.put(l);
            }
            bundle.put("consoleLogs", logs);

            // Network Requests
            JSONArray requests = new JSONArray();
            for (NetworkRequestItem req : activeTab.getNetworkRequests()) {
                JSONObject r = new JSONObject();
                r.put("url", req.getUrl());
                r.put("method", req.getMethod());
                r.put("status", req.getStatusCode());
                r.put("duration", req.getDuration());
                r.put("type", req.getResourceType());
                requests.put(r);
            }
            bundle.put("networkRequests", requests);

            String exportText = bundle.toString(2);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/json");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "F12 Full Diagnostic Bundle");
            shareIntent.putExtra(Intent.EXTRA_TEXT, exportText);
            controller.getContext().startActivity(Intent.createChooser(shareIntent, "Export Diagnostic Bundle"));
        } catch (Exception e) {
            Toast.makeText(controller.getContext(), "Export error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
        updateUaDisplay();
    }
}
