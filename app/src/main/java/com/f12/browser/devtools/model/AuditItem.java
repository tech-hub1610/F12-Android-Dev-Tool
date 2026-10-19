package com.f12.browser.devtools.model;

import org.json.JSONObject;

public class AuditItem {
    public enum Severity { PASS, WARN, FAIL }

    private final String category;
    private final String name;
    private final double score;
    private final Severity severity;
    private final String evidence;
    private final String fix;

    public AuditItem(String category, String name, double score, Severity severity, String evidence, String fix) {
        this.category = category;
        this.name = name;
        this.score = score;
        this.severity = severity;
        this.evidence = evidence;
        this.fix = fix;
    }

    public static AuditItem fromJson(JSONObject json) {
        String cat = json.optString("category", "General");
        String name = json.optString("name", "Audit Item");
        double score = json.optDouble("score", 1.0);
        String sevStr = json.optString("severity", "PASS").toUpperCase();
        Severity sev = Severity.PASS;
        if (sevStr.contains("WARN")) sev = Severity.WARN;
        else if (sevStr.contains("FAIL")) sev = Severity.FAIL;

        String evidence = json.optString("evidence", "");
        String fix = json.optString("fix", "");

        return new AuditItem(cat, name, score, sev, evidence, fix);
    }

    public String getCategory() { return category; }
    public String getName() { return name; }
    public double getScore() { return score; }
    public Severity getSeverity() { return severity; }
    public String getEvidence() { return evidence; }
    public String getFix() { return fix; }
}
