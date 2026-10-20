package com.f12.browser.devtools.panels;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.f12.browser.R;
import com.f12.browser.browser.BrowserTab;
import com.f12.browser.devtools.DevToolsController;
import com.f12.browser.devtools.model.PerformanceMetrics;
import com.f12.browser.util.FormatUtils;
import org.json.JSONObject;

public class PerformancePanel implements DevToolsPanel {
    private final DevToolsController controller;
    private final View root;

    private final Button btnRecord;
    private final Button btnRefresh;
    private final TextView textRecordingStatus;
    private final TextView textNavigation;
    private final TextView textPaint;
    private final TextView textComposition;
    private final TextView textMemory;

    private boolean isRecording = false;
    private long recordStartTime = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (isRecording) {
                long elapsed = (System.currentTimeMillis() - recordStartTime) / 1000;
                textRecordingStatus.setText("● Recording in progress: " + elapsed + "s elapsed (observing page interactions & frame rates)");
                handler.postDelayed(this, 1000);
            }
        }
    };

    public PerformancePanel(DevToolsController controller) {
        this.controller = controller;
        Context context = controller.getContext();
        root = LayoutInflater.from(context).inflate(R.layout.panel_performance, null, false);

        btnRecord = root.findViewById(R.id.btn_record_perf);
        btnRefresh = root.findViewById(R.id.btn_refresh_perf);
        textRecordingStatus = root.findViewById(R.id.text_recording_status);
        textNavigation = root.findViewById(R.id.text_perf_navigation);
        textPaint = root.findViewById(R.id.text_perf_paint);
        textComposition = root.findViewById(R.id.text_perf_composition);
        textMemory = root.findViewById(R.id.text_perf_memory);

        btnRefresh.setOnClickListener(v -> loadMetrics());

        btnRecord.setOnClickListener(v -> {
            if (!isRecording) {
                isRecording = true;
                recordStartTime = System.currentTimeMillis();
                btnRecord.setText("■ STOP RECORDING");
                btnRecord.setBackgroundResource(R.drawable.bg_button_dark);
                btnRecord.setTextColor(0xFFFFFFFF);
                textRecordingStatus.setVisibility(View.VISIBLE);
                handler.post(timerRunnable);
            } else {
                isRecording = false;
                long totalSecs = (System.currentTimeMillis() - recordStartTime) / 1000;
                btnRecord.setText("● START RECORDING");
                btnRecord.setBackgroundResource(R.drawable.bg_button_white);
                btnRecord.setTextColor(0xFF000000);
                textRecordingStatus.setText("✔ Recording finished: " + totalSecs + "s profile captured.");
                handler.removeCallbacks(timerRunnable);
                loadMetrics();
            }
        });
    }

    public void loadMetrics() {
        BrowserTab activeTab = controller.getActiveTab();
        if (activeTab == null || activeTab.getWebView() == null) return;

        activeTab.getWebView().evaluateJavascript("window.__F12__ ? window.__F12__.getPerformanceMetrics() : null", res -> {
            if (res == null || res.equals("null")) return;
            try {
                String clean = res;
                if (clean.startsWith("\"") && clean.endsWith("\"")) {
                    clean = new JSONObject("{ \"d\": " + clean + " }").getString("d");
                }
                JSONObject obj = new JSONObject(clean);
                PerformanceMetrics m = PerformanceMetrics.fromJson(obj);
                displayMetrics(m);
            } catch (Exception ignored) {}
        });
    }

    private void displayMetrics(PerformanceMetrics m) {
        // Navigation Timing
        String nav = "DNS Lookup Time:        " + FormatUtils.formatDuration(m.dns) + "\n"
                + "TCP Connection Time:    " + FormatUtils.formatDuration(m.tcp) + "\n"
                + "Time to First Byte:     " + FormatUtils.formatDuration(m.ttfb) + "\n"
                + "DOM Content Loaded:     " + FormatUtils.formatDuration(m.domContentLoaded) + "\n"
                + "Full Page Load Time:    " + FormatUtils.formatDuration(m.loadComplete);
        textNavigation.setText(nav);

        // Paint Timing
        String paint = "First Paint (FP):              " + (m.firstPaint > 0 ? (m.firstPaint + "ms") : "N/A") + "\n"
                + "First Contentful Paint (FCP):  " + (m.firstContentfulPaint > 0 ? (m.firstContentfulPaint + "ms") : "N/A");
        textPaint.setText(paint);

        // Page Composition
        String comp = "Total DOM Element Count:  " + m.domCount + "\n"
                + "Script Tags:              " + m.scriptsCount + "\n"
                + "Stylesheets / Styles:     " + m.stylesCount + "\n"
                + "Image Elements:           " + m.imagesCount;
        textComposition.setText(comp);

        // Memory
        if (m.totalJsHeapMb > 0) {
            String mem = "Used JS Heap:   " + m.usedJsHeapMb + " MB\n"
                    + "Total JS Heap:  " + m.totalJsHeapMb + " MB\n"
                    + "Heap Utilization: " + Math.round((m.usedJsHeapMb / m.totalJsHeapMb) * 100) + "%";
            textMemory.setText(mem);
        } else {
            textMemory.setText("Standard WebView Runtime (Chrome Engine)\nHeap size metrics are governed by Android WebView process boundaries.");
        }
    }

    @Override
    public View getView() { return root; }

    @Override
    public void onTabActivated() { refresh(); }

    @Override
    public void onTabDeactivated() {
        if (isRecording) {
            isRecording = false;
            handler.removeCallbacks(timerRunnable);
            btnRecord.setText("● START RECORDING");
            btnRecord.setBackgroundResource(R.drawable.bg_button_white);
            btnRecord.setTextColor(0xFF000000);
            textRecordingStatus.setVisibility(View.GONE);
        }
    }

    @Override
    public void refresh() {
        loadMetrics();
    }
}
