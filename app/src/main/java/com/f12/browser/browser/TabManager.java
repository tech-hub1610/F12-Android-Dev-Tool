package com.f12.browser.browser;

import android.content.Context;
import com.f12.browser.devtools.DevToolsController;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TabManager {
    public interface TabListener {
        void onTabCreated(BrowserTab tab);
        void onTabSelected(BrowserTab tab);
        void onTabClosed(BrowserTab tab);
        void onTabCountChanged(int count);
    }

    private final Context context;
    private final DevToolsController devToolsController;
    private final TabListener tabListener;
    private final List<BrowserTab> tabs = new ArrayList<>();
    private int activeTabIndex = -1;

    public TabManager(Context context, DevToolsController devToolsController, TabListener tabListener) {
        this.context = context;
        this.devToolsController = devToolsController;
        this.tabListener = tabListener;
    }

    public BrowserTab newTab(String initialUrl) {
        String id = UUID.randomUUID().toString();
        BrowserTab tab = new BrowserTab(context, id, devToolsController);
        tabs.add(tab);
        activeTabIndex = tabs.size() - 1;

        if (tabListener != null) {
            tabListener.onTabCreated(tab);
            tabListener.onTabSelected(tab);
            tabListener.onTabCountChanged(tabs.size());
        }

        if (initialUrl != null && !initialUrl.isEmpty() && !initialUrl.equalsIgnoreCase(com.f12.browser.util.UrlUtils.DASHBOARD_URL)) {
            tab.setCurrentUrl(initialUrl);
            tab.getWebView().loadUrl(initialUrl);
        } else {
            tab.setCurrentUrl(com.f12.browser.util.UrlUtils.DASHBOARD_URL);
            tab.setCurrentTitle("Dashboard");
            tab.getWebView().loadUrl("about:blank");
        }
        return tab;
    }

    public void selectTab(int index) {
        if (index >= 0 && index < tabs.size()) {
            activeTabIndex = index;
            if (tabListener != null) {
                tabListener.onTabSelected(tabs.get(index));
            }
        }
    }

    public void closeTab(int index) {
        if (index < 0 || index >= tabs.size()) return;
        BrowserTab toClose = tabs.get(index);
        tabs.remove(index);
        toClose.destroy();

        if (tabs.isEmpty()) {
            newTab(com.f12.browser.util.UrlUtils.DASHBOARD_URL);
        } else {
            if (activeTabIndex >= tabs.size()) {
                activeTabIndex = tabs.size() - 1;
            }
            if (tabListener != null) {
                tabListener.onTabClosed(toClose);
                tabListener.onTabSelected(tabs.get(activeTabIndex));
                tabListener.onTabCountChanged(tabs.size());
            }
        }
    }

    public BrowserTab getActiveTab() {
        if (activeTabIndex >= 0 && activeTabIndex < tabs.size()) {
            return tabs.get(activeTabIndex);
        }
        return null;
    }

    public List<BrowserTab> getTabs() {
        return tabs;
    }

    public int getActiveTabIndex() {
        return activeTabIndex;
    }

    public int getTabCount() {
        return tabs.size();
    }
}
