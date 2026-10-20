package com.f12.browser.devtools.panels;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import com.f12.browser.R;
import com.f12.browser.browser.BrowserTab;
import com.f12.browser.devtools.DevToolsController;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SourcesPanel implements DevToolsPanel {
    private final DevToolsController controller;
    private final View root;

    private final Button btnSelectResource;
    private final EditText searchInput;
    private final TextView textLineNumbers;
    private final TextView textCodeContent;

    public static class SourceItem {
        public final String type;
        public final String url;
        public final String title;
        public String content;

        public SourceItem(String type, String url, String title, String content) {
            this.type = type;
            this.url = url;
            this.title = title;
            this.content = content;
        }
    }

    private final List<SourceItem> sourcesList = new ArrayList<>();
    private SourceItem currentSource = null;

    public SourcesPanel(DevToolsController controller) {
        this.controller = controller;
        Context context = controller.getContext();
        root = LayoutInflater.from(context).inflate(R.layout.panel_sources, null, false);

        btnSelectResource = root.findViewById(R.id.btn_select_resource);
        searchInput = root.findViewById(R.id.source_search);
        textLineNumbers = root.findViewById(R.id.source_line_numbers);
        textCodeContent = root.findViewById(R.id.source_code_content);

        btnSelectResource.setOnClickListener(v -> showResourcePicker());

        root.findViewById(R.id.btn_copy_source).setOnClickListener(v -> copySource());

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { highlightSearch(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    public void loadSources() {
        BrowserTab activeTab = controller.getActiveTab();
        if (activeTab == null || activeTab.getWebView() == null) return;

        activeTab.getWebView().evaluateJavascript("window.__F12__ ? window.__F12__.getPageSources() : null", res -> {
            if (res == null || res.equals("null")) return;
            try {
                String clean = res;
                if (clean.startsWith("\"") && clean.endsWith("\"")) {
                    clean = new JSONObject("{ \"d\": " + clean + " }").getString("d");
                }
                JSONArray arr = new JSONArray(clean);
                sourcesList.clear();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    sourcesList.add(new SourceItem(
                            obj.optString("type"),
                            obj.optString("url"),
                            obj.optString("title"),
                            obj.optString("content")
                    ));
                }

                if (!sourcesList.isEmpty()) {
                    displaySource(sourcesList.get(0));
                }
            } catch (Exception ignored) {}
        });
    }

    private void showResourcePicker() {
        if (sourcesList.isEmpty()) {
            Toast.makeText(controller.getContext(), "No resources loaded yet", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] titles = new String[sourcesList.size()];
        for (int i = 0; i < sourcesList.size(); i++) {
            titles[i] = "[" + sourcesList.get(i).type.toUpperCase() + "] " + sourcesList.get(i).title;
        }

        new AlertDialog.Builder(controller.getContext())
                .setTitle("Select Source Resource")
                .setItems(titles, (dialog, which) -> {
                    displaySource(sourcesList.get(which));
                })
                .show();
    }

    private void displaySource(SourceItem item) {
        currentSource = item;
        btnSelectResource.setText(item.title);

        if (item.content == null || item.content.isEmpty()) {
            textCodeContent.setText("Loading resource from " + item.url + "...");
            fetchExternalResource(item);
        } else {
            renderCode(item.content);
        }
    }

    private void fetchExternalResource(SourceItem item) {
        new Thread(() -> {
            try {
                URL u = new URL(item.url);
                HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                int maxLines = 1000;
                int count = 0;
                while ((line = br.readLine()) != null && count++ < maxLines) {
                    sb.append(line).append("\n");
                }
                br.close();
                item.content = sb.toString();

                controller.getHandler().post(() -> {
                    if (currentSource == item) {
                        renderCode(item.content);
                    }
                });
            } catch (Exception e) {
                controller.getHandler().post(() -> {
                    if (currentSource == item) {
                        renderCode("// Failed to fetch external resource: " + e.getMessage());
                    }
                });
            }
        }).start();
    }

    private void renderCode(String code) {
        if (code == null) code = "";
        String[] lines = code.split("\n");
        StringBuilder lineNumSb = new StringBuilder();
        for (int i = 1; i <= lines.length; i++) {
            lineNumSb.append(i).append("\n");
        }
        textLineNumbers.setText(lineNumSb.toString());
        textCodeContent.setText(code);
    }

    private void highlightSearch(String query) {
        if (currentSource == null || currentSource.content == null) return;
        String content = currentSource.content;
        if (query == null || query.isEmpty()) {
            textCodeContent.setText(content);
            return;
        }

        SpannableString spannable = new SpannableString(content);
        String lowerContent = content.toLowerCase();
        String lowerQ = query.toLowerCase();
        int idx = 0;
        while ((idx = lowerContent.indexOf(lowerQ, idx)) != -1) {
            spannable.setSpan(new BackgroundColorSpan(0xFF444444), idx, idx + query.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            idx += query.length();
        }
        textCodeContent.setText(spannable);
    }

    private void copySource() {
        if (currentSource != null && currentSource.content != null) {
            ClipboardManager cm = (ClipboardManager) controller.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("Source Code", currentSource.content));
                Toast.makeText(controller.getContext(), "Source copied to clipboard", Toast.LENGTH_SHORT).show();
            }
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
        loadSources();
    }
}
