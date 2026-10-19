package com.f12.browser.devtools.panels;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.f12.browser.R;
import com.f12.browser.browser.BrowserTab;
import com.f12.browser.devtools.DevToolsController;
import com.f12.browser.devtools.model.DomNode;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ElementsPanel implements DevToolsPanel {
    private final DevToolsController controller;
    private final View root;

    private final RecyclerView recyclerDomTree;
    private final DomTreeAdapter treeAdapter;
    private final TextView textBreadcrumbs;
    private final EditText domSearch;
    private final ImageButton btnInspectTap;

    // Subtab views
    private final TextView subtabHtml;
    private final TextView subtabStyles;
    private final TextView subtabBoxModel;
    private final TextView subtabAttrs;
    private final View viewHtml;
    private final View viewStyles;
    private final View viewBoxModel;
    private final View viewAttrs;

    private final TextView textOuterHtml;
    private final TextView textStyles;
    private final TextView textBoxModel;
    private final TextView textAttributes;

    private DomNode rootDomNode = null;
    private final List<FlattenedNode> flattenedNodes = new ArrayList<>();
    private DomNode selectedNode = null;
    private boolean isTapInspectActive = false;

    public ElementsPanel(DevToolsController controller) {
        this.controller = controller;
        Context context = controller.getContext();
        root = LayoutInflater.from(context).inflate(R.layout.panel_elements, null, false);

        recyclerDomTree = root.findViewById(R.id.recycler_dom_tree);
        recyclerDomTree.setLayoutManager(new LinearLayoutManager(context));
        treeAdapter = new DomTreeAdapter();
        recyclerDomTree.setAdapter(treeAdapter);

        textBreadcrumbs = root.findViewById(R.id.dom_breadcrumbs);
        domSearch = root.findViewById(R.id.dom_search);
        btnInspectTap = root.findViewById(R.id.btn_inspect_tap);

        subtabHtml = root.findViewById(R.id.subtab_html);
        subtabStyles = root.findViewById(R.id.subtab_styles);
        subtabBoxModel = root.findViewById(R.id.subtab_boxmodel);
        subtabAttrs = root.findViewById(R.id.subtab_attrs);

        viewHtml = root.findViewById(R.id.view_subtab_html);
        viewStyles = root.findViewById(R.id.view_subtab_styles);
        viewBoxModel = root.findViewById(R.id.view_subtab_boxmodel);
        viewAttrs = root.findViewById(R.id.view_subtab_attrs);

        textOuterHtml = root.findViewById(R.id.text_element_outerhtml);
        textStyles = root.findViewById(R.id.text_element_styles);
        textBoxModel = root.findViewById(R.id.text_box_model_diagram);
        textAttributes = root.findViewById(R.id.text_element_attributes);

        initSubtabs();
        initActions();
    }

    private void initSubtabs() {
        View.OnClickListener listener = v -> {
            subtabHtml.setBackgroundResource(R.drawable.bg_tab_unselected);
            subtabHtml.setTextColor(0xFFCCCCCC);
            subtabStyles.setBackgroundResource(R.drawable.bg_tab_unselected);
            subtabStyles.setTextColor(0xFFCCCCCC);
            subtabBoxModel.setBackgroundResource(R.drawable.bg_tab_unselected);
            subtabBoxModel.setTextColor(0xFFCCCCCC);
            subtabAttrs.setBackgroundResource(R.drawable.bg_tab_unselected);
            subtabAttrs.setTextColor(0xFFCCCCCC);

            viewHtml.setVisibility(View.GONE);
            viewStyles.setVisibility(View.GONE);
            viewBoxModel.setVisibility(View.GONE);
            viewAttrs.setVisibility(View.GONE);

            if (v.getId() == R.id.subtab_html) {
                subtabHtml.setBackgroundResource(R.drawable.bg_tab_selected);
                subtabHtml.setTextColor(0xFF000000);
                viewHtml.setVisibility(View.VISIBLE);
            } else if (v.getId() == R.id.subtab_styles) {
                subtabStyles.setBackgroundResource(R.drawable.bg_tab_selected);
                subtabStyles.setTextColor(0xFF000000);
                viewStyles.setVisibility(View.VISIBLE);
            } else if (v.getId() == R.id.subtab_boxmodel) {
                subtabBoxModel.setBackgroundResource(R.drawable.bg_tab_selected);
                subtabBoxModel.setTextColor(0xFF000000);
                viewBoxModel.setVisibility(View.VISIBLE);
            } else if (v.getId() == R.id.subtab_attrs) {
                subtabAttrs.setBackgroundResource(R.drawable.bg_tab_selected);
                subtabAttrs.setTextColor(0xFF000000);
                viewAttrs.setVisibility(View.VISIBLE);
            }
        };

        subtabHtml.setOnClickListener(listener);
        subtabStyles.setOnClickListener(listener);
        subtabBoxModel.setOnClickListener(listener);
        subtabAttrs.setOnClickListener(listener);
    }

    private void initActions() {
        root.findViewById(R.id.btn_refresh_dom).setOnClickListener(v -> loadDomTree());

        btnInspectTap.setOnClickListener(v -> {
            isTapInspectActive = !isTapInspectActive;
            btnInspectTap.setBackgroundResource(isTapInspectActive ? R.drawable.bg_button_white : R.drawable.bg_button_dark);
            evaluateJs("window.__F12__ && window.__F12__.setInspectorActive(" + isTapInspectActive + ")", null);
            Toast.makeText(controller.getContext(), isTapInspectActive ? "Tap any element on the page to inspect" : "Tap inspect deactivated", Toast.LENGTH_SHORT).show();
        });

        domSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { searchNodes(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        root.findViewById(R.id.btn_edit_html).setOnClickListener(v -> showEditHtmlDialog());
        root.findViewById(R.id.btn_delete_element).setOnClickListener(v -> deleteSelectedElement());
        root.findViewById(R.id.btn_copy_html).setOnClickListener(v -> copyHtml());

        root.findViewById(R.id.btn_add_style).setOnClickListener(v -> showAddStyleDialog());
        root.findViewById(R.id.btn_add_attribute).setOnClickListener(v -> showAddAttributeDialog());
    }

    public void loadDomTree() {
        evaluateJs("window.__F12__ ? window.__F12__.getDomTree(6) : null", result -> {
            if (result == null || result.equals("null")) return;
            try {
                String clean = result;
                if (clean.startsWith("\"") && clean.endsWith("\"")) {
                    clean = new JSONObject("{ \"d\": " + clean + " }").getString("d");
                }
                JSONObject rootObj = new JSONObject(clean);
                rootDomNode = DomNode.fromJson(rootObj);
                if (rootDomNode != null) {
                    rootDomNode.setExpanded(true);
                    for (DomNode child : rootDomNode.getChildren()) {
                        child.setExpanded(true); // expand body/head
                    }
                }
                rebuildFlattenedList();
                if (rootDomNode != null && selectedNode == null) {
                    selectNode(rootDomNode);
                }
            } catch (Exception e) {
                // Parse error
            }
        });
    }

    private void rebuildFlattenedList() {
        flattenedNodes.clear();
        if (rootDomNode != null) {
            flatten(rootDomNode, 0);
        }
        treeAdapter.notifyDataSetChanged();
    }

    private void flatten(DomNode node, int depth) {
        flattenedNodes.add(new FlattenedNode(node, depth));
        if (node.isExpanded()) {
            for (DomNode child : node.getChildren()) {
                if (child.getNodeType() == 1) { // Only show element nodes in tree
                    flatten(child, depth + 1);
                }
            }
        }
    }

    private void selectNode(DomNode node) {
        selectedNode = node;
        treeAdapter.notifyDataSetChanged();

        textBreadcrumbs.setText(buildBreadcrumb(node));

        // Highlight in webview
        evaluateJs("window.__F12__ && window.__F12__.highlightElement('" + node.getF12Id() + "')", null);

        // Fetch element details
        evaluateJs("window.__F12__ ? window.__F12__.getElementDetails('" + node.getF12Id() + "') : null", res -> {
            if (res == null || res.equals("null")) return;
            try {
                String clean = res;
                if (clean.startsWith("\"") && clean.endsWith("\"")) {
                    clean = new JSONObject("{ \"d\": " + clean + " }").getString("d");
                }
                JSONObject details = new JSONObject(clean);
                updateDetailsUI(details);
            } catch (Exception ignored) {}
        });
    }

    private String buildBreadcrumb(DomNode node) {
        StringBuilder sb = new StringBuilder();
        sb.append(node.getTagName());
        if (!node.getId().isEmpty()) sb.append("#").append(node.getId());
        else if (!node.getClassName().isEmpty()) sb.append(".").append(node.getClassName().split(" ")[0]);
        return sb.toString();
    }

    private void updateDetailsUI(JSONObject details) {
        // Outer HTML
        String outer = details.optString("outerHTML", "");
        textOuterHtml.setText(outer);

        // Styles
        JSONObject styles = details.optJSONObject("styles");
        if (styles != null) {
            StringBuilder sb = new StringBuilder();
            Iterator<String> keys = styles.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                sb.append(k).append(": ").append(styles.optString(k)).append(";\n");
            }
            textStyles.setText(sb.toString().trim());
        } else {
            textStyles.setText("(No computed styles)");
        }

        // Box Model
        JSONObject box = details.optJSONObject("boxModel");
        if (box != null) {
            JSONObject m = box.optJSONObject("margin");
            JSONObject b = box.optJSONObject("border");
            JSONObject p = box.optJSONObject("padding");
            JSONObject c = box.optJSONObject("content");

            String diagram = "┌─────────────────────────────┐\n"
                    + "│ MARGIN: " + (m != null ? m.optString("top") : "") + "             │\n"
                    + "│  ┌───────────────────────┐  │\n"
                    + "│  │ BORDER: " + (b != null ? b.optString("top") : "") + "           │  │\n"
                    + "│  │  ┌─────────────────┐  │  │\n"
                    + "│  │  │ PADDING: " + (p != null ? p.optString("top") : "") + "      │  │  │\n"
                    + "│  │  │   " + (c != null ? (c.optString("width") + " x " + c.optString("height")) : "") + "   │  │  │\n"
                    + "│  │  │ PAD-BOTTOM: " + (p != null ? p.optString("bottom") : "") + "  │  │  │\n"
                    + "│  │  └─────────────────┘  │  │\n"
                    + "│  │ BORDER-BOTTOM: " + (b != null ? b.optString("bottom") : "") + " │  │\n"
                    + "│  └───────────────────────┘  │\n"
                    + "│ MARGIN-BOTTOM: " + (m != null ? m.optString("bottom") : "") + "         │\n"
                    + "└─────────────────────────────┘";
            textBoxModel.setText(diagram);
        } else {
            textBoxModel.setText("(Box model unavailable)");
        }

        // Attributes
        JSONObject attrs = details.optJSONObject("attributes");
        if (attrs != null && attrs.length() > 0) {
            StringBuilder sb = new StringBuilder();
            Iterator<String> keys = attrs.keys();
            while (keys.hasNext()) {
                String k = keys.next();
                sb.append(k).append(" = \"").append(attrs.optString(k)).append("\"\n");
            }
            textAttributes.setText(sb.toString().trim());
        } else {
            textAttributes.setText("(No attributes)");
        }
    }

    private void showEditHtmlDialog() {
        if (selectedNode == null) return;
        Context context = controller.getContext();
        EditText edit = new EditText(context);
        edit.setBackgroundResource(R.drawable.bg_code_box);
        edit.setTextColor(0xFFFFFFFF);
        edit.setTextSize(12);
        edit.setText(textOuterHtml.getText().toString());
        edit.setMinLines(6);

        new AlertDialog.Builder(context)
                .setTitle("Edit Element HTML")
                .setView(edit)
                .setPositiveButton("Apply Live", (dialog, which) -> {
                    String newHtml = edit.getText().toString().replace("'", "\\'").replace("\n", " ");
                    evaluateJs("window.__F12__ && window.__F12__.updateElementHTML('" + selectedNode.getF12Id() + "', '" + newHtml + "')", res -> {
                        loadDomTree();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddStyleDialog() {
        if (selectedNode == null) return;
        Context context = controller.getContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 16);

        EditText propEdit = new EditText(context);
        propEdit.setHint("Property (e.g. background-color, color, font-size)");
        propEdit.setTextColor(0xFFFFFFFF);
        propEdit.setHintTextColor(0xFF666666);
        layout.addView(propEdit);

        EditText valEdit = new EditText(context);
        valEdit.setHint("Value (e.g. #FFFFFF, 18px, none)");
        valEdit.setTextColor(0xFFFFFFFF);
        valEdit.setHintTextColor(0xFF666666);
        layout.addView(valEdit);

        new AlertDialog.Builder(context)
                .setTitle("Add / Modify CSS Style")
                .setView(layout)
                .setPositiveButton("Apply", (dialog, which) -> {
                    String prop = propEdit.getText().toString().trim();
                    String val = valEdit.getText().toString().trim();
                    if (!prop.isEmpty()) {
                        evaluateJs("window.__F12__ && window.__F12__.updateElementStyle('" + selectedNode.getF12Id() + "', '" + prop + "', '" + val + "')", res -> {
                            selectNode(selectedNode);
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddAttributeDialog() {
        if (selectedNode == null) return;
        Context context = controller.getContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 16);

        EditText nameEdit = new EditText(context);
        nameEdit.setHint("Attribute Name (e.g. class, title, href)");
        nameEdit.setTextColor(0xFFFFFFFF);
        nameEdit.setHintTextColor(0xFF666666);
        layout.addView(nameEdit);

        EditText valEdit = new EditText(context);
        valEdit.setHint("Value");
        valEdit.setTextColor(0xFFFFFFFF);
        valEdit.setHintTextColor(0xFF666666);
        layout.addView(valEdit);

        new AlertDialog.Builder(context)
                .setTitle("Set Element Attribute")
                .setView(layout)
                .setPositiveButton("Apply", (dialog, which) -> {
                    String name = nameEdit.getText().toString().trim();
                    String val = valEdit.getText().toString().trim();
                    if (!name.isEmpty()) {
                        evaluateJs("var el = document.querySelector('[data-f12-id=\"" + selectedNode.getF12Id() + "\"]'); if (el) el.setAttribute('" + name + "', '" + val + "');", res -> {
                            selectNode(selectedNode);
                        });
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSelectedElement() {
        if (selectedNode == null) return;
        new AlertDialog.Builder(controller.getContext())
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete <" + selectedNode.getTagName() + "> from the live page?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    evaluateJs("window.__F12__ && window.__F12__.removeElement('" + selectedNode.getF12Id() + "')", res -> {
                        selectedNode = null;
                        loadDomTree();
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void copyHtml() {
        String html = textOuterHtml.getText().toString();
        ClipboardManager cm = (ClipboardManager) controller.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("Element HTML", html));
            Toast.makeText(controller.getContext(), "HTML copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    public void onElementSelectedFromPage(String f12Id) {
        if (rootDomNode != null) {
            DomNode target = findNodeById(rootDomNode, f12Id);
            if (target != null) {
                selectNode(target);
                // Ensure expanded up to target
                rebuildFlattenedList();
            }
        }
    }

    private DomNode findNodeById(DomNode parent, String f12Id) {
        if (parent.getF12Id().equals(f12Id)) return parent;
        for (DomNode c : parent.getChildren()) {
            DomNode found = findNodeById(c, f12Id);
            if (found != null) return found;
        }
        return null;
    }

    private void searchNodes(String query) {
        if (query == null || query.trim().isEmpty()) {
            rebuildFlattenedList();
            return;
        }
        String q = query.toLowerCase().trim();
        List<FlattenedNode> results = new ArrayList<>();
        for (FlattenedNode fn : flattenedNodes) {
            DomNode n = fn.node;
            if (n.getTagName().contains(q) || n.getId().toLowerCase().contains(q) || n.getClassName().toLowerCase().contains(q)) {
                results.add(fn);
            }
        }
        flattenedNodes.clear();
        flattenedNodes.addAll(results);
        treeAdapter.notifyDataSetChanged();
    }

    private void evaluateJs(String script, ValueCallback callback) {
        BrowserTab activeTab = controller.getActiveTab();
        if (activeTab != null && activeTab.getWebView() != null) {
            activeTab.getWebView().evaluateJavascript(script, res -> {
                if (callback != null) callback.onReceiveValue(res);
            });
        }
    }

    public interface ValueCallback {
        void onReceiveValue(String value);
    }

    @Override
    public View getView() { return root; }

    @Override
    public void onTabActivated() {
        refresh();
    }

    @Override
    public void onTabDeactivated() {
        if (isTapInspectActive) {
            isTapInspectActive = false;
            btnInspectTap.setBackgroundResource(R.drawable.bg_button_dark);
            evaluateJs("window.__F12__ && window.__F12__.setInspectorActive(false)", null);
        }
        evaluateJs("window.__F12__ && window.__F12__.clearHighlight()", null);
    }

    @Override
    public void refresh() {
        loadDomTree();
    }

    private static class FlattenedNode {
        public final DomNode node;
        public final int depth;
        public FlattenedNode(DomNode node, int depth) {
            this.node = node;
            this.depth = depth;
        }
    }

    private class DomTreeAdapter extends RecyclerView.Adapter<DomNodeViewHolder> {
        @NonNull
        @Override
        public DomNodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_dom_tree_node, parent, false);
            return new DomNodeViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull DomNodeViewHolder holder, int position) {
            FlattenedNode fn = flattenedNodes.get(position);
            holder.bind(fn, selectedNode == fn.node);

            holder.chevron.setOnClickListener(v -> {
                fn.node.setExpanded(!fn.node.isExpanded());
                rebuildFlattenedList();
            });

            holder.itemView.setOnClickListener(v -> selectNode(fn.node));
        }

        @Override
        public int getItemCount() {
            return flattenedNodes.size();
        }
    }

    private static class DomNodeViewHolder extends RecyclerView.ViewHolder {
        private final View indent;
        private final ImageView chevron;
        private final TextView tag;
        private final TextView idText;
        private final TextView classText;
        private final TextView childCount;

        public DomNodeViewHolder(@NonNull View itemView) {
            super(itemView);
            indent = itemView.findViewById(R.id.node_indent);
            chevron = itemView.findViewById(R.id.node_chevron);
            tag = itemView.findViewById(R.id.node_tag);
            idText = itemView.findViewById(R.id.node_id);
            classText = itemView.findViewById(R.id.node_class);
            childCount = itemView.findViewById(R.id.node_child_count);
        }

        public void bind(FlattenedNode fn, boolean isSelected) {
            int dpIndent = fn.depth * 14;
            float density = itemView.getResources().getDisplayMetrics().density;
            ViewGroup.LayoutParams lp = indent.getLayoutParams();
            lp.width = (int) (dpIndent * density);
            indent.setLayoutParams(lp);

            if (isSelected) {
                itemView.setBackgroundColor(0xFF262626);
            } else {
                itemView.setBackgroundColor(0x00000000);
            }

            DomNode n = fn.node;
            tag.setText(n.getTagName());

            if (!n.getId().isEmpty()) {
                idText.setVisibility(View.VISIBLE);
                idText.setText("#" + n.getId());
            } else {
                idText.setVisibility(View.GONE);
            }

            if (!n.getClassName().isEmpty()) {
                classText.setVisibility(View.VISIBLE);
                classText.setText("." + n.getClassName().replace(" ", "."));
            } else {
                classText.setVisibility(View.GONE);
            }

            if (n.getChildren().isEmpty()) {
                chevron.setVisibility(View.INVISIBLE);
                childCount.setVisibility(View.GONE);
            } else {
                chevron.setVisibility(View.VISIBLE);
                chevron.setImageResource(n.isExpanded() ? R.drawable.ic_chevron_down : R.drawable.ic_chevron_right);
                childCount.setVisibility(View.VISIBLE);
                childCount.setText(String.valueOf(n.getChildren().size()));
            }
        }
    }
}
