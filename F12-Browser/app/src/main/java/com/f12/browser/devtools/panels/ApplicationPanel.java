package com.f12.browser.devtools.panels;

import android.app.AlertDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.f12.browser.R;
import com.f12.browser.browser.BrowserTab;
import com.f12.browser.devtools.DevToolsController;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ApplicationPanel implements DevToolsPanel {
    private final DevToolsController controller;
    private final View root;

    private final TextView tabLocal;
    private final TextView tabSession;
    private final TextView tabCookies;
    private final TextView tabIndexedDb;

    private final EditText searchFilter;
    private final RecyclerView recyclerView;
    private final StorageAdapter adapter;

    public static class StorageEntry {
        public String key;
        public String value;
        public StorageEntry(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    private final List<StorageEntry> allEntries = new ArrayList<>();
    private final List<StorageEntry> filteredEntries = new ArrayList<>();
    private String currentStorageType = "local"; // local, session, cookies, indexeddb

    public ApplicationPanel(DevToolsController controller) {
        this.controller = controller;
        Context context = controller.getContext();
        root = LayoutInflater.from(context).inflate(R.layout.panel_application, null, false);

        tabLocal = root.findViewById(R.id.tab_storage_local);
        tabSession = root.findViewById(R.id.tab_storage_session);
        tabCookies = root.findViewById(R.id.tab_storage_cookies);
        tabIndexedDb = root.findViewById(R.id.tab_storage_indexeddb);

        searchFilter = root.findViewById(R.id.storage_search);
        recyclerView = root.findViewById(R.id.recycler_storage);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new StorageAdapter();
        recyclerView.setAdapter(adapter);

        initTabs();
        initActions();
    }

    private void initTabs() {
        View.OnClickListener listener = v -> {
            tabLocal.setBackgroundResource(R.drawable.bg_tab_unselected);
            tabLocal.setTextColor(0xFFCCCCCC);
            tabSession.setBackgroundResource(R.drawable.bg_tab_unselected);
            tabSession.setTextColor(0xFFCCCCCC);
            tabCookies.setBackgroundResource(R.drawable.bg_tab_unselected);
            tabCookies.setTextColor(0xFFCCCCCC);
            tabIndexedDb.setBackgroundResource(R.drawable.bg_tab_unselected);
            tabIndexedDb.setTextColor(0xFFCCCCCC);

            if (v.getId() == R.id.tab_storage_local) {
                currentStorageType = "local";
                tabLocal.setBackgroundResource(R.drawable.bg_tab_selected);
                tabLocal.setTextColor(0xFF000000);
            } else if (v.getId() == R.id.tab_storage_session) {
                currentStorageType = "session";
                tabSession.setBackgroundResource(R.drawable.bg_tab_selected);
                tabSession.setTextColor(0xFF000000);
            } else if (v.getId() == R.id.tab_storage_cookies) {
                currentStorageType = "cookies";
                tabCookies.setBackgroundResource(R.drawable.bg_tab_selected);
                tabCookies.setTextColor(0xFF000000);
            } else if (v.getId() == R.id.tab_storage_indexeddb) {
                currentStorageType = "indexeddb";
                tabIndexedDb.setBackgroundResource(R.drawable.bg_tab_selected);
                tabIndexedDb.setTextColor(0xFF000000);
            }
            loadStorage();
        };

        tabLocal.setOnClickListener(listener);
        tabSession.setOnClickListener(listener);
        tabCookies.setOnClickListener(listener);
        tabIndexedDb.setOnClickListener(listener);
    }

    private void initActions() {
        searchFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        root.findViewById(R.id.btn_refresh_storage).setOnClickListener(v -> loadStorage());
        root.findViewById(R.id.btn_add_storage).setOnClickListener(v -> showAddDialog());
        root.findViewById(R.id.btn_clear_storage).setOnClickListener(v -> showClearAllConfirm());
    }

    public void loadStorage() {
        BrowserTab activeTab = controller.getActiveTab();
        if (activeTab == null || activeTab.getWebView() == null) return;

        if (currentStorageType.equals("indexeddb")) {
            activeTab.getWebView().evaluateJavascript(
                    "(function() { if (window.indexedDB && indexedDB.databases) { return indexedDB.databases().then(function(dbs) { return JSON.stringify(dbs); }); } else { return '[]'; } })()",
                    res -> {
                        allEntries.clear();
                        try {
                            String clean = res;
                            if (clean != null && clean.startsWith("\"") && clean.endsWith("\"")) {
                                clean = new JSONObject("{ \"d\": " + clean + " }").getString("d");
                            }
                            JSONArray arr = new JSONArray(clean);
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject db = arr.getJSONObject(i);
                                allEntries.add(new StorageEntry("Database: " + db.optString("name"), "Version: " + db.optInt("version", 1)));
                            }
                        } catch (Exception ignored) {}
                        if (allEntries.isEmpty()) {
                            allEntries.add(new StorageEntry("IndexedDB", "No indexed databases found for active origin"));
                        }
                        applyFilter();
                    }
            );
            return;
        }

        activeTab.getWebView().evaluateJavascript("window.__F12__ ? window.__F12__.getStorageData() : null", res -> {
            if (res == null || res.equals("null")) return;
            try {
                String clean = res;
                if (clean.startsWith("\"") && clean.endsWith("\"")) {
                    clean = new JSONObject("{ \"d\": " + clean + " }").getString("d");
                }
                JSONObject rootData = new JSONObject(clean);
                allEntries.clear();

                if (currentStorageType.equals("local")) {
                    JSONObject localObj = rootData.optJSONObject("localStorage");
                    if (localObj != null) {
                        Iterator<String> keys = localObj.keys();
                        while (keys.hasNext()) {
                            String k = keys.next();
                            allEntries.add(new StorageEntry(k, localObj.optString(k)));
                        }
                    }
                } else if (currentStorageType.equals("session")) {
                    JSONObject sessionObj = rootData.optJSONObject("sessionStorage");
                    if (sessionObj != null) {
                        Iterator<String> keys = sessionObj.keys();
                        while (keys.hasNext()) {
                            String k = keys.next();
                            allEntries.add(new StorageEntry(k, sessionObj.optString(k)));
                        }
                    }
                } else if (currentStorageType.equals("cookies")) {
                    // Combine JS cookies and CookieManager cookies
                    String activeUrl = activeTab.getCurrentUrl();
                    String cmCookie = CookieManager.getInstance().getCookie(activeUrl);
                    if (cmCookie != null && !cmCookie.isEmpty()) {
                        String[] pairs = cmCookie.split(";");
                        for (String p : pairs) {
                            String[] kv = p.trim().split("=");
                            if (kv.length >= 2) {
                                allEntries.add(new StorageEntry(kv[0], kv[1]));
                            } else if (kv.length == 1) {
                                allEntries.add(new StorageEntry(kv[0], ""));
                            }
                        }
                    }
                }

                applyFilter();
            } catch (Exception ignored) {}
        });
    }

    private void applyFilter() {
        filteredEntries.clear();
        String q = searchFilter.getText().toString().trim().toLowerCase();
        for (StorageEntry entry : allEntries) {
            if (q.isEmpty() || entry.key.toLowerCase().contains(q) || entry.value.toLowerCase().contains(q)) {
                filteredEntries.add(entry);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showAddDialog() {
        Context context = controller.getContext();
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 16);

        EditText keyInput = new EditText(context);
        keyInput.setHint("Key");
        keyInput.setTextColor(0xFFFFFFFF);
        keyInput.setHintTextColor(0xFF666666);
        layout.addView(keyInput);

        EditText valInput = new EditText(context);
        valInput.setHint("Value");
        valInput.setTextColor(0xFFFFFFFF);
        valInput.setHintTextColor(0xFF666666);
        layout.addView(valInput);

        new AlertDialog.Builder(context)
                .setTitle("Add " + currentStorageType.toUpperCase() + " Entry")
                .setView(layout)
                .setPositiveButton("Save", (dialog, which) -> {
                    String k = keyInput.getText().toString().trim();
                    String v = valInput.getText().toString().trim();
                    if (!k.isEmpty()) {
                        saveEntry(k, v);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditDialog(StorageEntry entry) {
        Context context = controller.getContext();
        EditText valInput = new EditText(context);
        valInput.setText(entry.value);
        valInput.setTextColor(0xFFFFFFFF);
        valInput.setMinLines(3);
        valInput.setBackgroundResource(R.drawable.bg_code_box);

        new AlertDialog.Builder(context)
                .setTitle("Edit Key: " + entry.key)
                .setView(valInput)
                .setPositiveButton("Save", (dialog, which) -> {
                    saveEntry(entry.key, valInput.getText().toString());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveEntry(String key, String value) {
        BrowserTab activeTab = controller.getActiveTab();
        if (activeTab == null || activeTab.getWebView() == null) return;

        String safeVal = value.replace("'", "\\'").replace("\n", " ");
        if (currentStorageType.equals("local")) {
            activeTab.getWebView().evaluateJavascript("localStorage.setItem('" + key + "', '" + safeVal + "')", res -> loadStorage());
        } else if (currentStorageType.equals("session")) {
            activeTab.getWebView().evaluateJavascript("sessionStorage.setItem('" + key + "', '" + safeVal + "')", res -> loadStorage());
        } else if (currentStorageType.equals("cookies")) {
            String activeUrl = activeTab.getCurrentUrl();
            CookieManager.getInstance().setCookie(activeUrl, key + "=" + safeVal + "; path=/");
            loadStorage();
        }
    }

    private void deleteEntry(StorageEntry entry) {
        new AlertDialog.Builder(controller.getContext())
                .setTitle("Confirm Delete")
                .setMessage("Are you sure you want to delete stored key '" + entry.key + "'?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    BrowserTab activeTab = controller.getActiveTab();
                    if (activeTab != null && activeTab.getWebView() != null) {
                        if (currentStorageType.equals("local")) {
                            activeTab.getWebView().evaluateJavascript("localStorage.removeItem('" + entry.key + "')", res -> loadStorage());
                        } else if (currentStorageType.equals("session")) {
                            activeTab.getWebView().evaluateJavascript("sessionStorage.removeItem('" + entry.key + "')", res -> loadStorage());
                        } else if (currentStorageType.equals("cookies")) {
                            String activeUrl = activeTab.getCurrentUrl();
                            CookieManager.getInstance().setCookie(activeUrl, entry.key + "=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;");
                            loadStorage();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showClearAllConfirm() {
        new AlertDialog.Builder(controller.getContext())
                .setTitle("Clear All " + currentStorageType.toUpperCase())
                .setMessage("Are you sure you want to clear all " + currentStorageType + " items for this site? This action cannot be undone.")
                .setPositiveButton("Clear All", (dialog, which) -> {
                    BrowserTab activeTab = controller.getActiveTab();
                    if (activeTab != null && activeTab.getWebView() != null) {
                        if (currentStorageType.equals("local")) {
                            activeTab.getWebView().evaluateJavascript("localStorage.clear()", res -> loadStorage());
                        } else if (currentStorageType.equals("session")) {
                            activeTab.getWebView().evaluateJavascript("sessionStorage.clear()", res -> loadStorage());
                        } else if (currentStorageType.equals("cookies")) {
                            CookieManager.getInstance().removeAllCookies(null);
                            loadStorage();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public View getView() { return root; }

    @Override
    public void onTabActivated() { refresh(); }

    @Override
    public void onTabDeactivated() {}

    @Override
    public void refresh() {
        loadStorage();
    }

    private class StorageAdapter extends RecyclerView.Adapter<StorageViewHolder> {
        @NonNull
        @Override
        public StorageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_storage_entry, parent, false);
            return new StorageViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull StorageViewHolder holder, int position) {
            StorageEntry entry = filteredEntries.get(position);
            holder.bind(entry);
            holder.btnEdit.setOnClickListener(v -> showEditDialog(entry));
            holder.btnDelete.setOnClickListener(v -> deleteEntry(entry));
        }

        @Override
        public int getItemCount() {
            return filteredEntries.size();
        }
    }

    private static class StorageViewHolder extends RecyclerView.ViewHolder {
        private final TextView key;
        private final TextView val;
        private final ImageButton btnEdit;
        private final ImageButton btnDelete;

        public StorageViewHolder(@NonNull View itemView) {
            super(itemView);
            key = itemView.findViewById(R.id.storage_key);
            val = itemView.findViewById(R.id.storage_val);
            btnEdit = itemView.findViewById(R.id.btn_edit_entry);
            btnDelete = itemView.findViewById(R.id.btn_delete_entry);
        }

        public void bind(StorageEntry entry) {
            key.setText(entry.key);
            val.setText(entry.value);
        }
    }
}
