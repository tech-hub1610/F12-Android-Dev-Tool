package com.f12.browser.devtools;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.f12.browser.R;
import com.f12.browser.browser.BrowserTab;
import com.f12.browser.browser.SettingsManager;
import com.f12.browser.browser.TabManager;
import com.f12.browser.devtools.model.ConsoleMessageItem;
import com.f12.browser.devtools.model.NetworkRequestItem;
import com.f12.browser.devtools.panels.ApplicationPanel;
import com.f12.browser.devtools.panels.ConsolePanel;
import com.f12.browser.devtools.panels.DevToolsPanel;
import com.f12.browser.devtools.panels.ElementsPanel;
import com.f12.browser.devtools.panels.LighthousePanel;
import com.f12.browser.devtools.panels.MoreToolsPanel;
import com.f12.browser.devtools.panels.NetworkPanel;
import com.f12.browser.devtools.panels.PerformancePanel;
import com.f12.browser.devtools.panels.ResponsivePanel;
import com.f12.browser.devtools.panels.SecurityPanel;
import com.f12.browser.devtools.panels.SourcesPanel;
import com.f12.browser.util.UrlUtils;
import java.util.HashMap;
import java.util.Map;

public class DevToolsController {
    public interface CloseListener {
        void onCloseRequested();
    }

    private final Context context;
    private final SettingsManager settingsManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private TabManager tabManager;
    private CloseListener closeListener;

    private View workspaceView;
    private FrameLayout panelContainer;
    private TextView pageTitle;
    private TextView badgeErrors;

    private final Map<String, DevToolsPanel> panels = new HashMap<>();
    private final Map<String, TextView> tabButtons = new HashMap<>();
    private String activePanelKey = "elements";
    private int errorCount = 0;

    public DevToolsController(Context context, SettingsManager settingsManager) {
        this.context = context;
        this.settingsManager = settingsManager;
    }

    public void setTabManager(TabManager tabManager) {
        this.tabManager = tabManager;
    }

    public void setCloseListener(CloseListener closeListener) {
        this.closeListener = closeListener;
    }

    public void initWorkspace(View view) {
        this.workspaceView = view;
        this.panelContainer = view.findViewById(R.id.devtools_panel_container);
        this.pageTitle = view.findViewById(R.id.workspace_page_title);
        this.badgeErrors = view.findViewById(R.id.badge_errors_count);

        view.findViewById(R.id.btn_close_devtools).setOnClickListener(v -> {
            if (closeListener != null) {
                closeListener.onCloseRequested();
            }
        });

        // Initialize panels
        panels.put("elements", new ElementsPanel(this));
        panels.put("console", new ConsolePanel(this));
        panels.put("network", new NetworkPanel(this));
        panels.put("sources", new SourcesPanel(this));
        panels.put("performance", new PerformancePanel(this));
        panels.put("responsive", new ResponsivePanel(this));
        panels.put("application", new ApplicationPanel(this));
        panels.put("security", new SecurityPanel(this));
        panels.put("lighthouse", new LighthousePanel(this));
        panels.put("more", new MoreToolsPanel(this));

        // Initialize tab views
        tabButtons.put("elements", view.findViewById(R.id.tab_elements));
        tabButtons.put("console", view.findViewById(R.id.tab_console));
        tabButtons.put("network", view.findViewById(R.id.tab_network));
        tabButtons.put("sources", view.findViewById(R.id.tab_sources));
        tabButtons.put("performance", view.findViewById(R.id.tab_performance));
        tabButtons.put("responsive", view.findViewById(R.id.tab_responsive));
        tabButtons.put("application", view.findViewById(R.id.tab_application));
        tabButtons.put("security", view.findViewById(R.id.tab_security));
        tabButtons.put("lighthouse", view.findViewById(R.id.tab_lighthouse));
        tabButtons.put("more", view.findViewById(R.id.tab_more));

        for (Map.Entry<String, TextView> entry : tabButtons.entrySet()) {
            final String key = entry.getKey();
            entry.getValue().setOnClickListener(v -> selectPanel(key));
        }

        // Show default panel
        selectPanel("elements");
    }

    public void selectPanel(String key) {
        if (!panels.containsKey(key)) return;

        // Deactivate previous
        DevToolsPanel prev = panels.get(activePanelKey);
        if (prev != null) prev.onTabDeactivated();

        // Update Tab Button styles
        for (Map.Entry<String, TextView> entry : tabButtons.entrySet()) {
            if (entry.getKey().equals(key)) {
                entry.getValue().setBackgroundResource(R.drawable.bg_tab_selected);
                entry.getValue().setTextColor(0xFF000000);
            } else {
                entry.getValue().setBackgroundResource(R.drawable.bg_tab_unselected);
                entry.getValue().setTextColor(0xFFCCCCCC);
            }
        }

        // Ensure selected tab is visible in HorizontalScrollView
        if (workspaceView != null) {
            android.widget.HorizontalScrollView tabScroll = workspaceView.findViewById(R.id.devtools_tabs_scroll);
            TextView currentTab = tabButtons.get(key);
            if (tabScroll != null && currentTab != null) {
                tabScroll.post(() -> {
                    int scrollTarget = currentTab.getLeft() - (tabScroll.getWidth() - currentTab.getWidth()) / 2;
                    tabScroll.smoothScrollTo(Math.max(0, scrollTarget), 0);
                });
            }
        }

        activePanelKey = key;
        DevToolsPanel current = panels.get(key);
        if (current != null && panelContainer != null) {
            panelContainer.removeAllViews();
            panelContainer.addView(current.getView());
            current.onTabActivated();
        }
    }

    public void onWorkspaceOpened() {
        updateHeaderInfo();
        DevToolsPanel current = panels.get(activePanelKey);
        if (current != null) {
            current.refresh();
        }
    }

    public void updateHeaderInfo() {
        BrowserTab activeTab = getActiveTab();
        if (activeTab != null && pageTitle != null) {
            String domain = UrlUtils.extractDomain(activeTab.getCurrentUrl());
            pageTitle.setText(domain);
        }
        updateBadgeErrors();
    }

    private void updateBadgeErrors() {
        if (badgeErrors == null) return;
        BrowserTab activeTab = getActiveTab();
        if (activeTab != null) {
            int errs = 0;
            for (ConsoleMessageItem item : activeTab.getConsoleMessages()) {
                if (item.getLevel() == ConsoleMessageItem.Level.ERROR) errs++;
            }
            errorCount = errs;
            badgeErrors.setText(errorCount + (errorCount == 1 ? " Error" : " Errors"));
            badgeErrors.setTextColor(errorCount > 0 ? 0xFFFFFFFF : 0xFF888888);
        }
    }

    // Bridge hooks
    public void onBridgeReady() {
        updateHeaderInfo();
    }

    public void onConsoleMessage(ConsoleMessageItem item) {
        BrowserTab activeTab = getActiveTab();
        if (activeTab != null) {
            activeTab.addConsoleMessage(item);
        }
        ConsolePanel console = (ConsolePanel) panels.get("console");
        if (console != null) {
            console.addMessage(item);
        }
        updateBadgeErrors();
    }

    public void onConsoleClear() {
        BrowserTab activeTab = getActiveTab();
        if (activeTab != null) {
            activeTab.clearConsoleMessages();
        }
        ConsolePanel console = (ConsolePanel) panels.get("console");
        if (console != null) {
            console.clear();
        }
        updateBadgeErrors();
    }

    public void onNetworkRequest(NetworkRequestItem item) {
        BrowserTab activeTab = getActiveTab();
        if (activeTab != null) {
            activeTab.addNetworkRequest(item);
        }
        NetworkPanel network = (NetworkPanel) panels.get("network");
        if (network != null) {
            network.addRequest(item);
        }
    }

    public void onSubresourceIntercepted(BrowserTab tab, NetworkRequestItem item) {
        tab.addNetworkRequest(item);
        if (tab == getActiveTab()) {
            handler.post(() -> {
                NetworkPanel network = (NetworkPanel) panels.get("network");
                if (network != null) {
                    network.addRequest(item);
                }
            });
        }
    }

    public void onElementSelectedFromPage(String f12Id) {
        ElementsPanel elements = (ElementsPanel) panels.get("elements");
        if (elements != null) {
            elements.onElementSelectedFromPage(f12Id);
        }
    }

    public BrowserTab getActiveTab() {
        return tabManager != null ? tabManager.getActiveTab() : null;
    }

    public Context getContext() { return context; }
    public SettingsManager getSettingsManager() { return settingsManager; }
    public Handler getHandler() { return handler; }
}
