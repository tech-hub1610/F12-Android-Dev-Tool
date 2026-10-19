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
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.f12.browser.R;
import com.f12.browser.browser.BrowserTab;
import com.f12.browser.devtools.DevToolsController;
import com.f12.browser.devtools.model.ConsoleMessageItem;
import com.f12.browser.util.FormatUtils;
import java.util.ArrayList;
import java.util.List;

public class ConsolePanel implements DevToolsPanel {
    private final DevToolsController controller;
    private final View root;
    private final RecyclerView recyclerView;
    private final ConsoleAdapter adapter;
    private final EditText inputCommand;
    private final EditText searchFilter;
    private final Button btnFilterLevel;
    private final List<ConsoleMessageItem> allMessages = new ArrayList<>();
    private final List<ConsoleMessageItem> filteredMessages = new ArrayList<>();
    private final List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;

    private String currentLevelFilter = "ALL"; // ALL, ERROR, WARN, INFO, LOG

    public ConsolePanel(DevToolsController controller) {
        this.controller = controller;
        Context context = controller.getContext();
        root = LayoutInflater.from(context).inflate(R.layout.panel_console, null, false);

        recyclerView = root.findViewById(R.id.recycler_console);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        adapter = new ConsoleAdapter();
        recyclerView.setAdapter(adapter);

        inputCommand = root.findViewById(R.id.console_input);
        searchFilter = root.findViewById(R.id.console_search);
        btnFilterLevel = root.findViewById(R.id.btn_filter_level);

        searchFilter.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { applyFilter(); }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnFilterLevel.setOnClickListener(v -> showLevelFilterDialog());

        root.findViewById(R.id.btn_clear_console).setOnClickListener(v -> clear());
        root.findViewById(R.id.btn_copy_console).setOnClickListener(v -> copyAll());

        root.findViewById(R.id.btn_eval_js).setOnClickListener(v -> executeCommand());

        ImageButton btnPrev = root.findViewById(R.id.btn_history_prev);
        ImageButton btnNext = root.findViewById(R.id.btn_history_next);

        btnPrev.setOnClickListener(v -> {
            if (commandHistory.isEmpty()) return;
            if (historyIndex < commandHistory.size() - 1) {
                historyIndex++;
                inputCommand.setText(commandHistory.get(commandHistory.size() - 1 - historyIndex));
                inputCommand.setSelection(inputCommand.getText().length());
            }
        });

        btnNext.setOnClickListener(v -> {
            if (commandHistory.isEmpty()) return;
            if (historyIndex > 0) {
                historyIndex--;
                inputCommand.setText(commandHistory.get(commandHistory.size() - 1 - historyIndex));
                inputCommand.setSelection(inputCommand.getText().length());
            } else if (historyIndex == 0) {
                historyIndex = -1;
                inputCommand.setText("");
            }
        });
    }

    private void showLevelFilterDialog() {
        String[] levels = {"ALL", "ERROR", "WARN", "INFO", "LOG"};
        new AlertDialog.Builder(controller.getContext())
                .setTitle("Filter Console Level")
                .setItems(levels, (dialog, which) -> {
                    currentLevelFilter = levels[which];
                    btnFilterLevel.setText(currentLevelFilter);
                    applyFilter();
                })
                .show();
    }

    private void executeCommand() {
        String code = inputCommand.getText().toString().trim();
        if (code.isEmpty()) return;

        commandHistory.add(code);
        historyIndex = -1;
        inputCommand.setText("");

        // Add to console as input entry
        ConsoleMessageItem inputItem = new ConsoleMessageItem(
                ConsoleMessageItem.Level.LOG,
                "❯ " + code,
                "input",
                0,
                System.currentTimeMillis()
        );
        addMessage(inputItem);

        BrowserTab activeTab = controller.getActiveTab();
        if (activeTab != null && activeTab.getWebView() != null) {
            WebView webView = activeTab.getWebView();
            // Wrap in safe expression evaluation
            String wrapped = "(function() { try { var res = (" + code + "); return (res === undefined) ? 'undefined' : (typeof res === 'object' ? JSON.stringify(res) : String(res)); } catch(e) { return 'Error: ' + e.message; } })()";
            webView.evaluateJavascript(wrapped, value -> {
                String cleanVal = value != null ? value : "null";
                if (cleanVal.startsWith("\"") && cleanVal.endsWith("\"") && cleanVal.length() >= 2) {
                    cleanVal = cleanVal.substring(1, cleanVal.length() - 1)
                            .replace("\\\"", "\"")
                            .replace("\\n", "\n")
                            .replace("\\\\", "\\");
                }
                ConsoleMessageItem.Level lvl = cleanVal.startsWith("Error:") ?
                        ConsoleMessageItem.Level.ERROR : ConsoleMessageItem.Level.INFO;
                addMessage(new ConsoleMessageItem(
                        lvl,
                        "◀ " + cleanVal,
                        "evaluation",
                        0,
                        System.currentTimeMillis()
                ));
            });
        }
    }

    public void addMessage(ConsoleMessageItem item) {
        allMessages.add(item);
        if (matchesFilter(item)) {
            filteredMessages.add(item);
            adapter.notifyItemInserted(filteredMessages.size() - 1);
            recyclerView.scrollToPosition(filteredMessages.size() - 1);
        }
    }

    public void clear() {
        allMessages.clear();
        filteredMessages.clear();
        adapter.notifyDataSetChanged();
        BrowserTab activeTab = controller.getActiveTab();
        if (activeTab != null) {
            activeTab.clearConsoleMessages();
        }
    }

    private void copyAll() {
        StringBuilder sb = new StringBuilder();
        for (ConsoleMessageItem item : filteredMessages) {
            sb.append("[").append(item.getLevel()).append("] ")
                    .append(FormatUtils.formatTime(item.getTimestamp())).append(" ")
                    .append(item.getMessage()).append("\n");
        }
        ClipboardManager cm = (ClipboardManager) controller.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) {
            cm.setPrimaryClip(ClipData.newPlainText("Console Logs", sb.toString()));
            Toast.makeText(controller.getContext(), "Console logs copied to clipboard", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean matchesFilter(ConsoleMessageItem item) {
        if (!currentLevelFilter.equals("ALL")) {
            if (!item.getLevel().name().equalsIgnoreCase(currentLevelFilter)) {
                return false;
            }
        }
        String query = searchFilter.getText().toString().trim().toLowerCase();
        if (!query.isEmpty()) {
            return item.getMessage().toLowerCase().contains(query);
        }
        return true;
    }

    private void applyFilter() {
        filteredMessages.clear();
        for (ConsoleMessageItem item : allMessages) {
            if (matchesFilter(item)) {
                filteredMessages.add(item);
            }
        }
        adapter.notifyDataSetChanged();
        if (!filteredMessages.isEmpty()) {
            recyclerView.scrollToPosition(filteredMessages.size() - 1);
        }
    }

    @Override
    public View getView() { return root; }

    @Override
    public void onTabActivated() {
        refresh();
    }

    @Override
    public void onTabDeactivated() {}

    @Override
    public void refresh() {
        allMessages.clear();
        BrowserTab activeTab = controller.getActiveTab();
        if (activeTab != null) {
            allMessages.addAll(activeTab.getConsoleMessages());
        }
        applyFilter();
    }

    private class ConsoleAdapter extends RecyclerView.Adapter<ConsoleViewHolder> {
        @NonNull
        @Override
        public ConsoleViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_console_message, parent, false);
            return new ConsoleViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ConsoleViewHolder holder, int position) {
            ConsoleMessageItem item = filteredMessages.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return filteredMessages.size();
        }
    }

    private static class ConsoleViewHolder extends RecyclerView.ViewHolder {
        private final TextView levelBadge;
        private final TextView msgText;
        private final TextView msgTime;
        private final TextView msgSource;

        public ConsoleViewHolder(@NonNull View itemView) {
            super(itemView);
            levelBadge = itemView.findViewById(R.id.msg_level_badge);
            msgText = itemView.findViewById(R.id.msg_text);
            msgTime = itemView.findViewById(R.id.msg_time);
            msgSource = itemView.findViewById(R.id.msg_source);
        }

        public void bind(ConsoleMessageItem item) {
            levelBadge.setText(item.getLevel().name());
            msgText.setText(item.getMessage());
            msgTime.setText(FormatUtils.formatTime(item.getTimestamp()));

            // Styling based on level
            if (item.getLevel() == ConsoleMessageItem.Level.ERROR) {
                levelBadge.setBackgroundResource(R.drawable.bg_button_white);
                levelBadge.setTextColor(0xFF000000);
                msgText.setTextColor(0xFFFFFFFF);
            } else if (item.getLevel() == ConsoleMessageItem.Level.WARN) {
                levelBadge.setBackgroundResource(R.drawable.bg_badge);
                levelBadge.setTextColor(0xFFFFFFFF);
                msgText.setTextColor(0xFFEEEEEE);
            } else {
                levelBadge.setBackgroundResource(R.drawable.bg_badge);
                levelBadge.setTextColor(0xFFAAAAAA);
                msgText.setTextColor(0xFFDDDDDD);
            }

            if (item.getSource() != null && !item.getSource().isEmpty()) {
                msgSource.setVisibility(View.VISIBLE);
                msgSource.setText(item.getSource() + (item.getLineNumber() > 0 ? ":" + item.getLineNumber() : ""));
            } else {
                msgSource.setVisibility(View.GONE);
            }
        }
    }
}
