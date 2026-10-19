package com.f12.browser.devtools.panels;

import android.view.View;

public interface DevToolsPanel {
    View getView();
    void onTabActivated();
    void onTabDeactivated();
    void refresh();
}
