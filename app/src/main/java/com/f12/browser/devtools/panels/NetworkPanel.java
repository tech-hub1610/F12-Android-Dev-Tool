package com.f12.browser.devtools.panels;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.f12.browser.R;
import com.f12.browser.browser.BrowserTab;
import com.f12.browser.devtools.DevToolsController;
import com.f12.browser.devtools.model.NetworkRequestItem;
import com.f12.browser.util.FormatUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NetworkPanel implements DevToolsPanel {
    private final DevToolsController controller;
    private final View root;
    private final RecyclerView recyclerView;
    private final NetworkAdapter adapter;
    private final EditText searchFilter;
    private final Button btnFilterType;
    private final List<NetworkRequestItem> allRequests = new ArrayList<>();
    private final List<NetworkRequestItem> filteredRequests = new ArrayList<>();

    private String currentTypeFilter = "ALL"; // ALL, FETCH/XHR, SCRIPT, STYLESHEET, IMAGE, DOCUMENT, OTHER

    public NetworkPanel(DevToolsController controller) {
        this.controller = controller;
        Context context = controller.getContext();
        root = LayoutInflater.from(context).inflate(R.layout.panel_network, null, false);

        recyclerView = root.findViewById(R.id.recycler_network);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new NetworkAdapter();
        recyclerView.setAdapter(adapter);

        searchFilter = root.findViewById(R.id.network_search);
        btnFilterType = root.findViewById(R.id.btn_filter_type);

        searchFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnFilterType.setOnClickListener(v -> showTypeFilterDialog());

        root.findViewById(R.id.btn_clear_network).setOnClickListener(v -> clear());
        root.findViewById(R.id.btn_export_network).setOnClickListener(v -> exportHar());
    }

    private void showTypeFilterDialog() {
        String[] types = {"ALL", "FETCH/XHR", "SCRIPT", "STYLESHEET", "IMAGE", "DOCUMENT", "OTHER"};
        new AlertDialog.Builder(controller.getContext())
                .setTitle("Filter Resource Type")
                .setItems(types, (dialog, which) -> {
                    currentTypeFilter = types[which];
                    btnFilterType.setText(currentTypeFilter);
                    applyFilter();
                })
                .show();
    }

    public void addRequest(NetworkRequestItem item) {
        allRequests.add(item);
        if (matchesFilter(item)) {
            filteredRequests.add(item);
            adapter.notifyItemInserted(filteredRequests.size() - 1);
        }
    }

    public void clear() {
        allRequests.clear();
        filteredRequests.clear();
        adapter.notifyDataSetChanged();
        BrowserTab activeTab = controller.getActiveTab();
        if (activeTab != null) {
            activeTab.clearNetworkRequests();
        }
    }

    private void exportHar() {
        try {
            JSONObject rootJson = new JSONObject();
            JSONObject log = new JSONObject();
            log.put("version", "1.2");
            log.put("creator", new JSONObject().put("name", "F12 Browser").put("version", "1.0.0"));

            JSONArray entries = new JSONArray();
            for (NetworkRequestItem req : allRequests) {
                JSONObject entry = new JSONObject();
                entry.put("startedDateTime", FormatUtils.formatTime(req.getStartTime()));
                entry.put("time", req.getDuration());

                JSONObject requestObj = new JSONObject();
                requestObj.put("method", req.getMethod());
                requestObj.put("url", req.getUrl());
                JSONArray reqH = new JSONArray();
                for (Map.Entry<String, String> e : req.getRequestHeaders().entrySet()) {
                    reqH.put(new JSONObject().put("name", e.getKey()).put("value", e.getValue()));
                }
                requestObj.put("headers", reqH);
                requestObj.put("postData", new JSONObject().put("text", req.getRequestBody()));
                entry.put("request", requestObj);

                JSONObject responseObj = new JSONObject();
                responseObj.put("status", req.getStatusCode());
                responseObj.put("statusText", req.getStatusText());
                JSONArray respH = new JSONArray();
                for (Map.Entry<String, String> e : req.getResponseHeaders().entrySet()) {
                    respH.put(new JSONObject().put("name", e.getKey()).put("value", e.getValue()));
                }
                responseObj.put("headers", respH);
                responseObj.put("content", new JSONObject().put("text", req.getResponseBody()));
                entry.put("response", responseObj);

                entries.put(entry);
            }
            log.put("entries", entries);
            rootJson.put("log", log);

            String harReport = rootJson.toString(2);

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/json");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "F12 Network Diagnostic HAR");
            shareIntent.putExtra(Intent.EXTRA_TEXT, harReport);
            controller.getContext().startActivity(Intent.createChooser(shareIntent, "Export Network HAR"));
        } catch (Exception e) {
            Toast.makeText(controller.getContext(), "Error exporting HAR: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean matchesFilter(NetworkRequestItem item) {
        if (!currentTypeFilter.equals("ALL")) {
            String type = item.getResourceType().toLowerCase();
            if (currentTypeFilter.equals("FETCH/XHR") && !type.equals("fetch") && !type.equals("xhr")) {
                return false;
            } else if (currentTypeFilter.equals("SCRIPT") && !type.equals("script")) {
                return false;
            } else if (currentTypeFilter.equals("STYLESHEET") && !type.equals("stylesheet")) {
                return false;
            } else if (currentTypeFilter.equals("IMAGE") && !type.equals("image")) {
                return false;
            } else if (currentTypeFilter.equals("DOCUMENT") && !type.equals("document")) {
                return false;
            } else if (currentTypeFilter.equals("OTHER") && (type.equals("fetch") || type.equals("xhr") || type.equals("script") || type.equals("stylesheet") || type.equals("image") || type.equals("document"))) {
                return false;
            }
        }

        String query = searchFilter.getText().toString().trim().toLowerCase();
        if (!query.isEmpty()) {
            return item.getUrl().toLowerCase().contains(query);
        }
        return true;
    }

    private void applyFilter() {
        filteredRequests.clear();
        for (NetworkRequestItem req : allRequests) {
            if (matchesFilter(req)) {
                filteredRequests.add(req);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showRequestDetails(NetworkRequestItem item) {
        Context context = controller.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_network_details, null, false);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(view)
                .create();

        TextView title = view.findViewById(R.id.dialog_req_title);
        title.setText(item.getFileName());

        view.findViewById(R.id.dialog_req_close).setOnClickListener(v -> dialog.dismiss());

        // General
        TextView detailGeneral = view.findViewById(R.id.detail_general);
        String generalInfo = "Request URL: " + item.getUrl() + "\n"
                + "Request Method: " + item.getMethod() + "\n"
                + "Status Code: " + item.getStatusCode() + " " + item.getStatusText() + "\n"
                + "Duration: " + FormatUtils.formatDuration(item.getDuration()) + "\n"
                + "Type: " + item.getResourceType() + "\n"
                + "Time: " + FormatUtils.formatTime(item.getStartTime());
        detailGeneral.setText(generalInfo);

        // Request Headers
        TextView detailReqH = view.findViewById(R.id.detail_req_headers);
        if (item.getRequestHeaders().isEmpty()) {
            detailReqH.setText("(None recorded)");
        } else {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> e : item.getRequestHeaders().entrySet()) {
                sb.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
            }
            detailReqH.setText(sb.toString().trim());
        }

        // Request Body
        TextView detailReqBody = view.findViewById(R.id.detail_req_body);
        if (item.getRequestBody() != null && !item.getRequestBody().isEmpty()) {
            detailReqBody.setText(item.getRequestBody());
        } else {
            detailReqBody.setText("(Empty)");
        }

        // Response Headers
        TextView detailRespH = view.findViewById(R.id.detail_resp_headers);
        if (item.getResponseHeaders().isEmpty()) {
            detailRespH.setText("(None recorded)");
        } else {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> e : item.getResponseHeaders().entrySet()) {
                sb.append(e.getKey()).append(": ").append(e.getValue()).append("\n");
            }
            detailRespH.setText(sb.toString().trim());
        }

        // Response Body
        TextView detailRespBody = view.findViewById(R.id.detail_resp_body);
        final String respBodyFormatted = FormatUtils.formatJsonPretty(item.getResponseBody());
        if (respBodyFormatted.isEmpty()) {
            detailRespBody.setText("(No body content)");
        } else {
            detailRespBody.setText(respBodyFormatted);
        }

        view.findViewById(R.id.btn_copy_response).setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("Response Body", respBodyFormatted));
                Toast.makeText(context, "Response body copied", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    @Override
    public View getView() { return root; }

    @Override
    public void onTabActivated() { refresh(); }

    @Override
    public void onTabDeactivated() {}

    @Override
    public void refresh() {
        allRequests.clear();
        BrowserTab activeTab = controller.getActiveTab();
        if (activeTab != null) {
            allRequests.addAll(activeTab.getNetworkRequests());
        }
        applyFilter();
    }

    private class NetworkAdapter extends RecyclerView.Adapter<NetworkViewHolder> {
        @NonNull
        @Override
        public NetworkViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_network_request, parent, false);
            return new NetworkViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull NetworkViewHolder holder, int position) {
            NetworkRequestItem item = filteredRequests.get(position);
            holder.bind(item);
            holder.itemView.setOnClickListener(v -> showRequestDetails(item));
        }

        @Override
        public int getItemCount() {
            return filteredRequests.size();
        }
    }

    private static class NetworkViewHolder extends RecyclerView.ViewHolder {
        private final TextView reqStatus;
        private final TextView reqMethod;
        private final TextView reqName;
        private final TextView reqType;
        private final TextView reqTime;

        public NetworkViewHolder(@NonNull View itemView) {
            super(itemView);
            reqStatus = itemView.findViewById(R.id.req_status);
            reqMethod = itemView.findViewById(R.id.req_method);
            reqName = itemView.findViewById(R.id.req_name);
            reqType = itemView.findViewById(R.id.req_type);
            reqTime = itemView.findViewById(R.id.req_time);
        }

        public void bind(NetworkRequestItem item) {
            int code = item.getStatusCode();
            reqStatus.setText(String.valueOf(code));
            if (code >= 200 && code < 300) {
                reqStatus.setTextColor(0xFFFFFFFF);
            } else if (code >= 400 || code == 0) {
                reqStatus.setTextColor(0xFFCCCCCC);
            } else {
                reqStatus.setTextColor(0xFFAAAAAA);
            }

            reqMethod.setText(item.getMethod());
            reqName.setText(item.getFileName());
            reqType.setText(item.getResourceType());
            reqTime.setText(FormatUtils.formatDuration(item.getDuration()));
        }
    }
}
