package com.f12.browser.devtools.model;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DomNode {
    private final int nodeType;
    private final String f12Id;
    private final String tagName;
    private final String id;
    private final String className;
    private final Map<String, String> attributes;
    private final int childCount;
    private final String textContent;
    private final List<DomNode> children;
    private boolean isExpanded = false;

    public DomNode(int nodeType, String f12Id, String tagName, String id, String className,
                   Map<String, String> attributes, int childCount, String textContent,
                   List<DomNode> children) {
        this.nodeType = nodeType;
        this.f12Id = f12Id;
        this.tagName = tagName;
        this.id = id;
        this.className = className;
        this.attributes = attributes != null ? attributes : new HashMap<>();
        this.childCount = childCount;
        this.textContent = textContent != null ? textContent : "";
        this.children = children != null ? children : new ArrayList<>();
    }

    public static DomNode fromJson(JSONObject json) {
        if (json == null) return null;
        int nodeType = json.optInt("nodeType", 1);
        if (nodeType == 3) {
            return new DomNode(3, "", "", "", "", null, 0, json.optString("text", ""), null);
        }

        String f12Id = json.optString("f12Id", "");
        String tagName = json.optString("tagName", "");
        String id = json.optString("id", "");
        String className = json.optString("className", "");
        int childCount = json.optInt("childCount", 0);

        Map<String, String> attrs = new HashMap<>();
        JSONObject attrObj = json.optJSONObject("attributes");
        if (attrObj != null) {
            Iterator<String> keys = attrObj.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                attrs.put(k, attrObj.optString(k));
            }
        }

        List<DomNode> childList = new ArrayList<>();
        JSONArray cArr = json.optJSONArray("children");
        if (cArr != null) {
            for (int i = 0; i < cArr.length(); i++) {
                DomNode child = fromJson(cArr.optJSONObject(i));
                if (child != null) childList.add(child);
            }
        }

        return new DomNode(nodeType, f12Id, tagName, id, className, attrs, childCount, "", childList);
    }

    public int getNodeType() { return nodeType; }
    public String getF12Id() { return f12Id; }
    public String getTagName() { return tagName; }
    public String getId() { return id; }
    public String getClassName() { return className; }
    public Map<String, String> getAttributes() { return attributes; }
    public int getChildCount() { return childCount; }
    public String getTextContent() { return textContent; }
    public List<DomNode> getChildren() { return children; }
    public boolean isExpanded() { return isExpanded; }
    public void setExpanded(boolean expanded) { this.isExpanded = expanded; }

    public String getFormattedOpeningTag() {
        if (nodeType == 3) {
            return "\"" + textContent + "\"";
        }
        StringBuilder sb = new StringBuilder("<").append(tagName);
        if (!id.isEmpty()) {
            sb.append(" id=\"").append(id).append("\"");
        }
        if (!className.isEmpty()) {
            sb.append(" class=\"").append(className).append("\"");
        }
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            if (!entry.getKey().equals("id") && !entry.getKey().equals("class")) {
                sb.append(" ").append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"");
            }
        }
        sb.append(">");
        return sb.toString();
    }
}
