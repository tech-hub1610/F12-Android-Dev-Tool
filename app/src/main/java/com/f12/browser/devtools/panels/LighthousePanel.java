package com.f12.browser.devtools.panels;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.f12.browser.R;
import com.f12.browser.browser.BrowserTab;
import com.f12.browser.devtools.DevToolsController;
import com.f12.browser.devtools.model.AuditItem;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LighthousePanel implements DevToolsPanel {
    private final DevToolsController controller;
    private final View root;

    private final Button btnRun;
    private final ImageButton btnExport;
    private final TextView scoreSeo;
    private final TextView scoreA11y;
    private final TextView scorePractices;
    private final TextView scorePerf;
    private final TextView scoreMobile;

    private final RecyclerView recyclerView;
    private final AuditAdapter adapter;
    private final List<AuditItem> auditItems = new ArrayList<>();

    public LighthousePanel(DevToolsController controller) {
        this.controller = controller;
        Context context = controller.getContext();
        root = LayoutInflater.from(context).inflate(R.layout.panel_lighthouse, null, false);

        btnRun = root.findViewById(R.id.btn_run_audits);
        btnExport = root.findViewById(R.id.btn_export_audits);

        scoreSeo = root.findViewById(R.id.score_seo);
        scoreA11y = root.findViewById(R.id.score_a11y);
        scorePractices = root.findViewById(R.id.score_practices);
        scorePerf = root.findViewById(R.id.score_perf);
        scoreMobile = root.findViewById(R.id.score_mobile);

        recyclerView = root.findViewById(R.id.recycler_audits);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new AuditAdapter();
        recyclerView.setAdapter(adapter);

        btnRun.setOnClickListener(v -> runAudits());
        btnExport.setOnClickListener(v -> exportReport());
    }

    public void runAudits() {
        BrowserTab activeTab = controller.getActiveTab();
        if (activeTab == null || activeTab.getWebView() == null) return;

        btnRun.setText("RUNNING AUDIT CHECKS...");
        activeTab.getWebView().evaluateJavascript("window.__F12__ ? window.__F12__.runAudits() : null", res -> {
            btnRun.setText("RUN IN-APP AUDIT");
            if (res == null || res.equals("null")) return;
            try {
                String clean = res;
                if (clean.startsWith("\"") && clean.endsWith("\"")) {
                    clean = new JSONObject("{ \"d\": " + clean + " }").getString("d");
                }
                JSONArray arr = new JSONArray(clean);
                auditItems.clear();

                Map<String, List<Double>> categoryScores = new HashMap<>();

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    AuditItem item = AuditItem.fromJson(obj);
                    auditItems.add(item);

                    if (!categoryScores.containsKey(item.getCategory())) {
                        categoryScores.put(item.getCategory(), new ArrayList<>());
                    }
                    categoryScores.get(item.getCategory()).add(item.getScore());
                }

                updateScores(categoryScores);
                adapter.notifyDataSetChanged();
            } catch (Exception ignored) {}
        });
    }

    private void updateScores(Map<String, List<Double>> scoresMap) {
        scoreSeo.setText(calculatePercent(scoresMap.get("SEO")));
        scoreA11y.setText(calculatePercent(scoresMap.get("Accessibility")));
        scorePractices.setText(calculatePercent(scoresMap.get("Best Practices")));
        scorePerf.setText(calculatePercent(scoresMap.get("Performance")));
        scoreMobile.setText(calculatePercent(scoresMap.get("Mobile Usability")));
    }

    private String calculatePercent(List<Double> scores) {
        if (scores == null || scores.isEmpty()) return "--";
        double sum = 0;
        for (Double s : scores) sum += s;
        int pct = (int) Math.round((sum / scores.size()) * 100);
        return pct + "%";
    }

    private void exportReport() {
        if (auditItems.isEmpty()) {
            Toast.makeText(controller.getContext(), "Run an audit first before exporting", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== F12 IN-APP AUDIT REPORT ===\n\n");
        sb.append("Scores Summary:\n");
        sb.append("SEO: ").append(scoreSeo.getText()).append(" | ");
        sb.append("Accessibility: ").append(scoreA11y.getText()).append(" | ");
        sb.append("Best Practices: ").append(scorePractices.getText()).append(" | ");
        sb.append("Performance: ").append(scorePerf.getText()).append(" | ");
        sb.append("Mobile: ").append(scoreMobile.getText()).append("\n\n");
        sb.append("Detailed Findings:\n");

        for (AuditItem item : auditItems) {
            sb.append("[").append(item.getSeverity()).append("] ")
                    .append(item.getCategory()).append(" - ").append(item.getName()).append("\n")
                    .append("  Evidence: ").append(item.getEvidence()).append("\n")
                    .append("  Fix: ").append(item.getFix()).append("\n\n");
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "F12 Audit Report");
        shareIntent.putExtra(Intent.EXTRA_TEXT, sb.toString());
        controller.getContext().startActivity(Intent.createChooser(shareIntent, "Export Audit Report"));
    }

    @Override
    public View getView() { return root; }

    @Override
    public void onTabActivated() {
        if (auditItems.isEmpty()) {
            runAudits();
        }
    }

    @Override
    public void onTabDeactivated() {}

    @Override
    public void refresh() {
        runAudits();
    }

    private class AuditAdapter extends RecyclerView.Adapter<AuditViewHolder> {
        @NonNull
        @Override
        public AuditViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_audit_finding, parent, false);
            return new AuditViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull AuditViewHolder holder, int position) {
            AuditItem item = auditItems.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return auditItems.size();
        }
    }

    private static class AuditViewHolder extends RecyclerView.ViewHolder {
        private final TextView severityBadge;
        private final TextView category;
        private final TextView name;
        private final TextView evidence;
        private final TextView fix;

        public AuditViewHolder(@NonNull View itemView) {
            super(itemView);
            severityBadge = itemView.findViewById(R.id.audit_severity_badge);
            category = itemView.findViewById(R.id.audit_category);
            name = itemView.findViewById(R.id.audit_name);
            evidence = itemView.findViewById(R.id.audit_evidence);
            fix = itemView.findViewById(R.id.audit_fix);
        }

        public void bind(AuditItem item) {
            severityBadge.setText(item.getSeverity().name());
            category.setText(item.getCategory());
            name.setText(item.getName());
            evidence.setText("Evidence: " + item.getEvidence());
            fix.setText("Fix: " + item.getFix());

            Context ctx = itemView.getContext();
            if (item.getSeverity() == AuditItem.Severity.PASS) {
                severityBadge.setBackgroundResource(R.drawable.bg_button_white);
                severityBadge.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.bg_black));
            } else if (item.getSeverity() == AuditItem.Severity.WARN) {
                severityBadge.setBackgroundResource(R.drawable.bg_badge);
                severityBadge.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.console_warn_badge));
            } else {
                severityBadge.setBackgroundResource(R.drawable.bg_button_dark);
                severityBadge.setTextColor(androidx.core.content.ContextCompat.getColor(ctx, R.color.console_error_badge));
            }
        }
    }
}
