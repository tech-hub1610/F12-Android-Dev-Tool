package com.f12.browser;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.URLUtil;
import android.webkit.WebBackForwardList;
import android.webkit.WebHistoryItem;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.f12.browser.browser.BookmarkManager;
import com.f12.browser.browser.BrowserTab;
import com.f12.browser.browser.BrowserWebChromeClient;
import com.f12.browser.browser.BrowserWebViewClient;
import com.f12.browser.browser.HistoryManager;
import com.f12.browser.browser.SettingsManager;
import com.f12.browser.browser.TabManager;
import com.f12.browser.devtools.DevToolsController;
import com.f12.browser.google.GoogleDataManager;
import com.f12.browser.util.FormatUtils;
import com.f12.browser.util.UrlUtils;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements TabManager.TabListener {

    private static final int REQ_IMPORT_CHROME_BOOKMARKS = 3001;
    private static final int REQ_IMPORT_TAKEOUT_HISTORY = 3002;
    private static final int REQ_IMPORT_FULL_BACKUP = 3003;

    private SettingsManager settingsManager;
    private BookmarkManager bookmarkManager;
    private HistoryManager historyManager;
    private DevToolsController devToolsController;
    private TabManager tabManager;

    // UI elements
    private ImageButton btnHome;
    private EditText editUrl;
    private FrameLayout btnTabs;
    private TextView txtTabCount;
    private Button btnF12;
    private ImageButton btnMenu;
    private ProgressBar progressBar;
    private View nativeDashboard;
    private FrameLayout webviewContainer;
    private FrameLayout devtoolsContainer;

    // Dashboard dynamic containers
    private LinearLayout dashBookmarksContainer;
    private LinearLayout dashHistoryContainer;

    // Find in Page
    private View findBar;
    private EditText findInput;
    private TextView findMatchCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Enable cookies
        CookieManager.getInstance().setAcceptCookie(true);

        settingsManager = new SettingsManager(this);
        bookmarkManager = new BookmarkManager(this);
        historyManager = new HistoryManager(this);

        initViews();
        initDashboard();
        initDevTools();
        initTabManager();

        // Load initial tab with Native Dashboard
        tabManager.newTab(UrlUtils.DASHBOARD_URL);
    }

    private void initViews() {
        btnHome = findViewById(R.id.btn_home);
        editUrl = findViewById(R.id.edit_url_search);
        btnTabs = findViewById(R.id.btn_tabs);
        txtTabCount = findViewById(R.id.txt_tab_count);
        btnF12 = findViewById(R.id.btn_f12);
        btnMenu = findViewById(R.id.btn_menu);
        progressBar = findViewById(R.id.browser_progress_bar);
        nativeDashboard = findViewById(R.id.native_dashboard);
        webviewContainer = findViewById(R.id.webview_container);
        devtoolsContainer = findViewById(R.id.devtools_workspace_container);

        findBar = findViewById(R.id.find_in_page_bar);
        findInput = findViewById(R.id.find_input);
        findMatchCount = findViewById(R.id.find_match_count);

        btnHome.setOnClickListener(v -> showDashboard());

        editUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                String input = editUrl.getText().toString();
                String targetUrl = UrlUtils.smartUrl(input, settingsManager.getSearchEngine());
                hideKeyboard();
                openUrl(targetUrl);
                return true;
            }
            return false;
        });

        // Active Tab Switcher Button
        btnTabs.setOnClickListener(v -> showTabsDialog());
        btnTabs.setOnLongClickListener(v -> {
            tabManager.newTab(UrlUtils.DASHBOARD_URL);
            Toast.makeText(MainActivity.this, "New tab opened", Toast.LENGTH_SHORT).show();
            return true;
        });

        // The F12 Button - The flagship feature!
        btnF12.setOnClickListener(v -> toggleDevTools());

        btnMenu.setOnClickListener(this::showBrowserMenu);

        initFindInPage();
    }

    private void initDashboard() {
        dashBookmarksContainer = nativeDashboard.findViewById(R.id.dash_bookmarks_container);
        dashHistoryContainer = nativeDashboard.findViewById(R.id.dash_history_container);

        // Dashboard search input and Go button
        android.widget.EditText dashSearchInput = nativeDashboard.findViewById(R.id.dash_search_input);
        android.view.View dashBtnGo = nativeDashboard.findViewById(R.id.dash_btn_go);

        Runnable performDashSearch = () -> {
            String input = dashSearchInput.getText().toString().trim();
            if (input.isEmpty()) {
                input = editUrl.getText().toString().trim();
            }
            if (!input.isEmpty()) {
                String targetUrl = UrlUtils.smartUrl(input, settingsManager.getSearchEngine());
                dashSearchInput.setText("");
                hideKeyboard();
                openUrl(targetUrl);
            }
        };

        if (dashSearchInput != null) {
            dashSearchInput.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_SEARCH
                        || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_DOWN)) {
                    performDashSearch.run();
                    return true;
                }
                return false;
            });
        }

        if (dashBtnGo != null) {
            dashBtnGo.setOnClickListener(v -> performDashSearch.run());
        }

        // Speed Dial buttons
        nativeDashboard.findViewById(R.id.dial_google).setOnClickListener(v -> openUrl("https://www.google.com"));
        nativeDashboard.findViewById(R.id.dial_github).setOnClickListener(v -> openUrl("https://github.com"));
        nativeDashboard.findViewById(R.id.dial_mdn).setOnClickListener(v -> openUrl("https://developer.mozilla.org"));
        nativeDashboard.findViewById(R.id.dial_stackoverflow).setOnClickListener(v -> openUrl("https://stackoverflow.com"));
        nativeDashboard.findViewById(R.id.dial_caniuse).setOnClickListener(v -> openUrl("https://caniuse.com"));
        nativeDashboard.findViewById(R.id.dial_localhost).setOnClickListener(v -> openUrl("http://localhost:3000"));
        nativeDashboard.findViewById(R.id.dial_emulator).setOnClickListener(v -> openUrl("http://10.0.2.2:8080"));
        nativeDashboard.findViewById(R.id.dial_sandbox).setOnClickListener(v -> openUrl("file:///android_asset/home/index.html"));

        // View All links
        nativeDashboard.findViewById(R.id.dash_view_all_bookmarks).setOnClickListener(v -> showBookmarksDialog());
        nativeDashboard.findViewById(R.id.dash_view_all_history).setOnClickListener(v -> showHistoryDialog());

        refreshDashboardData();
    }

    private void refreshDashboardData() {
        // Refresh Bookmarks list in dashboard
        dashBookmarksContainer.removeAllViews();
        List<BookmarkManager.BookmarkItem> bookmarks = bookmarkManager.getBookmarks();
        if (bookmarks.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No bookmarks saved yet. Tap menu ⋮ > Bookmarks to save pages.");
            emptyText.setTextColor(0xFF666666);
            emptyText.setTextSize(11);
            emptyText.setPadding(0, 4, 0, 8);
            dashBookmarksContainer.addView(emptyText);
        } else {
            int max = Math.min(bookmarks.size(), 4);
            for (int i = 0; i < max; i++) {
                BookmarkManager.BookmarkItem bm = bookmarks.get(i);
                View item = LayoutInflater.from(this).inflate(R.layout.item_bookmark, dashBookmarksContainer, false);
                ((TextView) item.findViewById(R.id.bm_title)).setText(bm.title);
                ((TextView) item.findViewById(R.id.bm_url)).setText(bm.url);
                item.findViewById(R.id.btn_delete_bm).setVisibility(View.GONE);
                item.setOnClickListener(v -> openUrl(bm.url));
                dashBookmarksContainer.addView(item);
            }
        }

        // Refresh Recent History list in dashboard
        dashHistoryContainer.removeAllViews();
        List<HistoryManager.HistoryItem> history = historyManager.getHistory();
        if (history.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No browsing history yet. Explore websites to populate history.");
            emptyText.setTextColor(0xFF666666);
            emptyText.setTextSize(11);
            emptyText.setPadding(0, 4, 0, 8);
            dashHistoryContainer.addView(emptyText);
        } else {
            int max = Math.min(history.size(), 4);
            for (int i = 0; i < max; i++) {
                HistoryManager.HistoryItem h = history.get(i);
                View item = LayoutInflater.from(this).inflate(R.layout.item_history, dashHistoryContainer, false);
                ((TextView) item.findViewById(R.id.hist_title)).setText(h.title);
                ((TextView) item.findViewById(R.id.hist_url)).setText(h.url);
                ((TextView) item.findViewById(R.id.hist_time)).setText(FormatUtils.formatTime(h.timestamp));
                item.setOnClickListener(v -> openUrl(h.url));
                dashHistoryContainer.addView(item);
            }
        }
    }

    public void showDashboard() {
        webviewContainer.setVisibility(View.GONE);
        nativeDashboard.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        editUrl.setText("");
        editUrl.clearFocus();
        hideKeyboard();

        BrowserTab activeTab = tabManager.getActiveTab();
        if (activeTab != null) {
            activeTab.setCurrentUrl(UrlUtils.DASHBOARD_URL);
            activeTab.setCurrentTitle("Dashboard");
        }
        devToolsController.updateHeaderInfo();
        refreshDashboardData();
    }

    public void openUrl(String url) {
        if (url == null || url.isEmpty() || url.equalsIgnoreCase(UrlUtils.DASHBOARD_URL)) {
            showDashboard();
            return;
        }

        nativeDashboard.setVisibility(View.GONE);
        webviewContainer.setVisibility(View.VISIBLE);
        editUrl.setText(url);
        editUrl.clearFocus();
        hideKeyboard();
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setProgress(15);

        BrowserTab activeTab = tabManager.getActiveTab();
        if (activeTab != null && activeTab.getWebView() != null) {
            activeTab.setCurrentUrl(url);
            activeTab.getWebView().loadUrl(url);
        }
    }

    private void initFindInPage() {
        findInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                BrowserTab activeTab = tabManager.getActiveTab();
                if (activeTab != null && activeTab.getWebView() != null) {
                    activeTab.getWebView().findAllAsync(s.toString());
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        findViewById(R.id.btn_find_prev).setOnClickListener(v -> {
            BrowserTab activeTab = tabManager.getActiveTab();
            if (activeTab != null && activeTab.getWebView() != null) {
                activeTab.getWebView().findNext(false);
            }
        });

        findViewById(R.id.btn_find_next).setOnClickListener(v -> {
            BrowserTab activeTab = tabManager.getActiveTab();
            if (activeTab != null && activeTab.getWebView() != null) {
                activeTab.getWebView().findNext(true);
            }
        });

        findViewById(R.id.btn_find_close).setOnClickListener(v -> {
            findBar.setVisibility(View.GONE);
            BrowserTab activeTab = tabManager.getActiveTab();
            if (activeTab != null && activeTab.getWebView() != null) {
                activeTab.getWebView().clearMatches();
            }
        });
    }

    private void initDevTools() {
        devToolsController = new DevToolsController(this, settingsManager);
        View workspaceView = LayoutInflater.from(this).inflate(R.layout.layout_devtools_workspace, devtoolsContainer, false);
        devtoolsContainer.addView(workspaceView);
        devToolsController.initWorkspace(workspaceView);
        devToolsController.setCloseListener(() -> {
            devtoolsContainer.setVisibility(View.GONE);
        });
    }

    private void initTabManager() {
        tabManager = new TabManager(this, devToolsController, this);
        devToolsController.setTabManager(tabManager);
    }

    private void toggleDevTools() {
        if (devtoolsContainer.getVisibility() == View.VISIBLE) {
            devtoolsContainer.setVisibility(View.GONE);
        } else {
            devtoolsContainer.setVisibility(View.VISIBLE);
            devToolsController.onWorkspaceOpened();
        }
    }

    private void showBrowserMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "+ New Tab");
        popup.getMenu().add(0, 2, 1, "Tabs (" + tabManager.getTabCount() + ")");
        popup.getMenu().add(0, 3, 2, "Bookmarks");
        popup.getMenu().add(0, 4, 3, "History");

        BrowserTab activeTab = tabManager.getActiveTab();
        boolean isDesktop = activeTab != null && activeTab.isDesktopMode();
        popup.getMenu().add(0, 5, 4, isDesktop ? "✔ Desktop Site" : "Desktop Site");

        popup.getMenu().add(0, 6, 5, "Find in Page");
        popup.getMenu().add(0, 7, 6, "Share Page");
        popup.getMenu().add(0, 8, 7, "Reload");
        popup.getMenu().add(0, 9, 8, "Settings");

        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == 1) {
                tabManager.newTab(UrlUtils.DASHBOARD_URL);
            } else if (id == 2) {
                showTabsDialog();
            } else if (id == 3) {
                showBookmarksDialog();
            } else if (id == 4) {
                showHistoryDialog();
            } else if (id == 5) {
                if (activeTab != null) {
                    activeTab.setDesktopMode(!activeTab.isDesktopMode());
                    activeTab.getWebView().reload();
                }
            } else if (id == 6) {
                findBar.setVisibility(View.VISIBLE);
                findInput.requestFocus();
            } else if (id == 7) {
                shareCurrentUrl();
            } else if (id == 8) {
                if (activeTab != null && activeTab.getWebView() != null) {
                    activeTab.getWebView().reload();
                }
            } else if (id == 9) {
                showSettingsDialog();
            }
            return true;
        });
        popup.show();
    }

    private void shareCurrentUrl() {
        BrowserTab activeTab = tabManager.getActiveTab();
        if (activeTab != null && activeTab.getCurrentUrl() != null && !activeTab.getCurrentUrl().equals(UrlUtils.DASHBOARD_URL)) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, activeTab.getCurrentTitle());
            shareIntent.putExtra(Intent.EXTRA_TEXT, activeTab.getCurrentUrl());
            startActivity(Intent.createChooser(shareIntent, "Share Web Page"));
        } else {
            Toast.makeText(this, "Cannot share Dashboard", Toast.LENGTH_SHORT).show();
        }
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    // ==========================================
    // TabManager.TabListener Callbacks
    // ==========================================
    @Override
    public void onTabCreated(BrowserTab tab) {
        WebView webView = tab.getWebView();

        // Clients
        webView.setWebViewClient(new BrowserWebViewClient(tab, devToolsController, new BrowserWebViewClient.PageListener() {
            @Override
            public void onPageStarted(String url) {
                if (tab == tabManager.getActiveTab()) {
                    if (url != null && !url.equalsIgnoreCase(UrlUtils.DASHBOARD_URL) && !url.equals("about:blank") && !url.startsWith("data:text/html")) {
                        nativeDashboard.setVisibility(View.GONE);
                        webviewContainer.setVisibility(View.VISIBLE);
                        progressBar.setVisibility(View.VISIBLE);
                        progressBar.setProgress(10);
                        editUrl.setText(url);
                    } else if (url == null || url.equalsIgnoreCase(UrlUtils.DASHBOARD_URL)) {
                        showDashboard();
                    }
                }
            }

            @Override
            public void onPageFinished(String url, String title) {
                if (tab == tabManager.getActiveTab()) {
                    progressBar.setVisibility(View.GONE);
                    if (url != null && !url.equalsIgnoreCase(UrlUtils.DASHBOARD_URL) && !url.equals("about:blank") && !url.startsWith("data:text/html")) {
                        editUrl.setText(url);
                    }
                    devToolsController.updateHeaderInfo();
                }
                if (url != null && !url.equalsIgnoreCase(UrlUtils.DASHBOARD_URL) && !url.equals("about:blank") && !url.startsWith("data:text/html")) {
                    historyManager.addHistory(title, url);
                }
            }

            @Override
            public void onPageError(String failingUrl, String description) {
                if (tab == tabManager.getActiveTab()) {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onDashboardRequested() {
                runOnUiThread(() -> showDashboard());
            }
        }));

        webView.setWebChromeClient(new BrowserWebChromeClient(tab, devToolsController, new BrowserWebChromeClient.ChromeListener() {
            @Override
            public void onProgressChanged(int progress) {
                if (tab == tabManager.getActiveTab() && webviewContainer.getVisibility() == View.VISIBLE) {
                    progressBar.setProgress(progress);
                    if (progress >= 100) {
                        progressBar.setVisibility(View.GONE);
                    } else {
                        progressBar.setVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onTitleChanged(String title) {
                if (tab == tabManager.getActiveTab()) {
                    devToolsController.updateHeaderInfo();
                }
            }
        }));

        // Find Listener
        webView.setFindListener((activeMatchOrdinal, numberOfMatches, isDoneCounting) -> {
            if (isDoneCounting) {
                findMatchCount.setText((numberOfMatches > 0 ? (activeMatchOrdinal + 1) : 0) + "/" + numberOfMatches);
            }
        });

        // Download handling
        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                request.setMimeType(mimetype);
                String fileName = URLUtil.guessFileName(url, contentDisposition, mimetype);
                request.setTitle(fileName);
                request.setDescription("Downloading file...");
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

                DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                if (dm != null) {
                    dm.enqueue(request);
                    Toast.makeText(MainActivity.this, "Downloading " + fileName, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(MainActivity.this, "Download error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onTabSelected(BrowserTab tab) {
        webviewContainer.removeAllViews();
        webviewContainer.addView(tab.getWebView(), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        String url = tab.getCurrentUrl();
        if (url == null || url.isEmpty() || url.equalsIgnoreCase(UrlUtils.DASHBOARD_URL) || url.equals("about:blank")) {
            showDashboard();
        } else {
            nativeDashboard.setVisibility(View.GONE);
            webviewContainer.setVisibility(View.VISIBLE);
            editUrl.setText(url);
            progressBar.setProgress(tab.getProgress());
            progressBar.setVisibility(tab.getProgress() < 100 ? View.VISIBLE : View.GONE);
        }
        devToolsController.updateHeaderInfo();
    }

    @Override
    public void onTabClosed(BrowserTab tab) {
        // Handled in TabManager
    }

    @Override
    public void onTabCountChanged(int count) {
        if (txtTabCount != null) {
            txtTabCount.setText(count > 99 ? ":D" : String.valueOf(count));
        }
    }

    // ==========================================
    // Dialogs: Tabs, Bookmarks, History, Settings
    // ==========================================
    private void showTabsDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_tabs, null, false);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        RecyclerView recycler = view.findViewById(R.id.recycler_tabs);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        TabsAdapter adapter = new TabsAdapter(tabManager.getTabs(), (tab, pos, isClose) -> {
            if (isClose) {
                tabManager.closeTab(pos);
                dialog.dismiss();
                if (tabManager.getTabCount() > 0) {
                    showTabsDialog();
                }
            } else {
                tabManager.selectTab(pos);
                dialog.dismiss();
            }
        });
        recycler.setAdapter(adapter);

        view.findViewById(R.id.btn_new_tab).setOnClickListener(v -> {
            tabManager.newTab(UrlUtils.DASHBOARD_URL);
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showBookmarksDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_bookmarks, null, false);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        RecyclerView recycler = view.findViewById(R.id.recycler_bookmarks);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        List<BookmarkManager.BookmarkItem> bookmarks = bookmarkManager.getBookmarks();
        BookmarksAdapter adapter = new BookmarksAdapter(bookmarks, (item, isDelete) -> {
            if (isDelete) {
                bookmarkManager.removeBookmark(item.url);
                bookmarks.remove(item);
                recycler.getAdapter().notifyDataSetChanged();
                refreshDashboardData();
            } else {
                openUrl(item.url);
                dialog.dismiss();
            }
        });
        recycler.setAdapter(adapter);

        view.findViewById(R.id.btn_add_current_bm).setOnClickListener(v -> {
            BrowserTab activeTab = tabManager.getActiveTab();
            if (activeTab != null && activeTab.getCurrentUrl() != null && !activeTab.getCurrentUrl().equals(UrlUtils.DASHBOARD_URL)) {
                bookmarkManager.addBookmark(activeTab.getCurrentTitle(), activeTab.getCurrentUrl());
                Toast.makeText(MainActivity.this, "Bookmarked current page", Toast.LENGTH_SHORT).show();
                refreshDashboardData();
                dialog.dismiss();
            } else {
                Toast.makeText(MainActivity.this, "Cannot bookmark Dashboard", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btn_bm_import_chrome).setOnClickListener(v -> {
            pickFileForImport(REQ_IMPORT_CHROME_BOOKMARKS);
            dialog.dismiss();
        });

        view.findViewById(R.id.btn_bm_export_chrome).setOnClickListener(v -> {
            exportChromeBookmarks();
        });

        dialog.show();
    }

    private void showHistoryDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_history, null, false);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        RecyclerView recycler = view.findViewById(R.id.recycler_history);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        List<HistoryManager.HistoryItem> historyList = new ArrayList<>(historyManager.getHistory());
        HistoryAdapter adapter = new HistoryAdapter(historyList, item -> {
            openUrl(item.url);
            dialog.dismiss();
        });
        recycler.setAdapter(adapter);

        EditText search = view.findViewById(R.id.history_search);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String q = s.toString().toLowerCase().trim();
                historyList.clear();
                for (HistoryManager.HistoryItem item : historyManager.getHistory()) {
                    if (q.isEmpty() || item.title.toLowerCase().contains(q) || item.url.toLowerCase().contains(q)) {
                        historyList.add(item);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        view.findViewById(R.id.btn_clear_history).setOnClickListener(v -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Clear History")
                    .setMessage("Are you sure you want to delete all browsing history?")
                    .setPositiveButton("Clear", (d, w) -> {
                        historyManager.clearHistory();
                        historyList.clear();
                        adapter.notifyDataSetChanged();
                        refreshDashboardData();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        view.findViewById(R.id.btn_hist_import_takeout).setOnClickListener(v -> {
            pickFileForImport(REQ_IMPORT_TAKEOUT_HISTORY);
            dialog.dismiss();
        });

        view.findViewById(R.id.btn_hist_export_takeout).setOnClickListener(v -> {
            exportTakeoutHistory();
        });

        dialog.show();
    }

    private void showSettingsDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null, false);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        Button btnEngine = view.findViewById(R.id.btn_setting_engine);
        btnEngine.setText(getSearchEngineName(settingsManager.getSearchEngine()));
        btnEngine.setOnClickListener(v -> {
            String[] names = {"Google", "DuckDuckGo", "Bing"};
            String[] urls = {SettingsManager.SEARCH_GOOGLE, SettingsManager.SEARCH_DUCKDUCKGO, SettingsManager.SEARCH_BING};
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Select Search Engine")
                    .setItems(names, (d, which) -> {
                        settingsManager.setSearchEngine(urls[which]);
                        btnEngine.setText(names[which]);
                    })
                    .show();
        });

        EditText editHome = view.findViewById(R.id.edit_setting_homepage);
        editHome.setText(settingsManager.getHomepage());

        CheckBox chkPreserve = view.findViewById(R.id.chk_preserve_logs);
        chkPreserve.setChecked(settingsManager.isPreserveLogs());
        chkPreserve.setOnCheckedChangeListener((btn, isChecked) -> {
            settingsManager.setPreserveLogs(isChecked);
        });

        // Google Integration & Data Sync buttons in Settings
        view.findViewById(R.id.btn_setting_google_account).setOnClickListener(v -> {
            boolean signedIn = GoogleDataManager.isGoogleSignedIn();
            openUrl(signedIn ? GoogleDataManager.GOOGLE_MANAGE_ACCOUNT_URL : GoogleDataManager.GOOGLE_SIGN_IN_URL);
            dialog.dismiss();
        });

        view.findViewById(R.id.btn_setting_google_takeout).setOnClickListener(v -> {
            openUrl(GoogleDataManager.GOOGLE_TAKEOUT_URL);
            dialog.dismiss();
        });

        view.findViewById(R.id.btn_setting_import_chrome_bm).setOnClickListener(v -> {
            pickFileForImport(REQ_IMPORT_CHROME_BOOKMARKS);
            dialog.dismiss();
        });

        view.findViewById(R.id.btn_setting_export_chrome_bm).setOnClickListener(v -> {
            exportChromeBookmarks();
        });

        view.findViewById(R.id.btn_setting_import_history).setOnClickListener(v -> {
            pickFileForImport(REQ_IMPORT_TAKEOUT_HISTORY);
            dialog.dismiss();
        });

        view.findViewById(R.id.btn_setting_export_backup).setOnClickListener(v -> {
            exportFullBackup();
        });

        view.findViewById(R.id.btn_clear_data).setOnClickListener(v -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Clear Browsing Data")
                    .setMessage("Clear browser cache, cookies, and history?")
                    .setPositiveButton("Clear All", (d, which) -> {
                        historyManager.clearHistory();
                        CookieManager.getInstance().removeAllCookies(null);
                        BrowserTab activeTab = tabManager.getActiveTab();
                        if (activeTab != null && activeTab.getWebView() != null) {
                            activeTab.getWebView().clearCache(true);
                        }
                        refreshDashboardData();
                        Toast.makeText(MainActivity.this, "Browsing data cleared", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        dialog.setOnDismissListener(d -> {
            String newHome = editHome.getText().toString().trim();
            if (!newHome.isEmpty()) {
                settingsManager.setHomepage(newHome);
            }
        });

        dialog.show();
    }

    private String getSearchEngineName(String url) {
        if (url.contains("duckduckgo")) return "DuckDuckGo";
        if (url.contains("bing")) return "Bing";
        return "Google";
    }

    // ==========================================
    // Google & Data Import/Export Utilities
    // ==========================================
    private void pickFileForImport(int requestCode) {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(intent, "Select File to Import"), requestCode);
        } catch (Exception e) {
            Toast.makeText(this, "File picker error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportChromeBookmarks() {
        try {
            String html = GoogleDataManager.exportToChromeBookmarksHtml(bookmarkManager.getBookmarks());
            String fileName = "f12_chrome_bookmarks_" + System.currentTimeMillis() + ".html";
            java.io.File file = GoogleDataManager.saveFileToDownloads(fileName, html);
            Toast.makeText(this, "Bookmarks exported to Downloads: " + file.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportTakeoutHistory() {
        try {
            String json = GoogleDataManager.exportToGoogleTakeoutJson(historyManager.getHistory());
            String fileName = "f12_takeout_history_" + System.currentTimeMillis() + ".json";
            java.io.File file = GoogleDataManager.saveFileToDownloads(fileName, json);
            Toast.makeText(this, "History exported to Downloads: " + file.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void exportFullBackup() {
        try {
            String json = GoogleDataManager.exportFullDataBackup(bookmarkManager, historyManager, settingsManager);
            String fileName = "f12_browser_backup_" + System.currentTimeMillis() + ".json";
            java.io.File file = GoogleDataManager.saveFileToDownloads(fileName, json);
            Toast.makeText(this, "Backup saved to Downloads: " + file.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Backup failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            if (requestCode == REQ_IMPORT_CHROME_BOOKMARKS) {
                try {
                    String content = GoogleDataManager.readContentFromUri(getContentResolver(), uri);
                    List<BookmarkManager.BookmarkItem> items = GoogleDataManager.parseChromeBookmarks(content);
                    int added = bookmarkManager.importBookmarks(items);
                    refreshDashboardData();
                    Toast.makeText(this, "Imported " + added + " bookmarks from Google Chrome!", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Bookmark import failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQ_IMPORT_TAKEOUT_HISTORY) {
                try {
                    String content = GoogleDataManager.readContentFromUri(getContentResolver(), uri);
                    List<HistoryManager.HistoryItem> items = GoogleDataManager.parseTakeoutHistory(content);
                    int added = historyManager.importHistory(items);
                    refreshDashboardData();
                    Toast.makeText(this, "Imported " + added + " history items from Takeout JSON!", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, "History import failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else if (requestCode == REQ_IMPORT_FULL_BACKUP) {
                try {
                    String content = GoogleDataManager.readContentFromUri(getContentResolver(), uri);
                    org.json.JSONObject root = new org.json.JSONObject(content);
                    int bmAdded = 0;
                    if (root.has("bookmarks")) {
                        org.json.JSONArray bArr = root.getJSONArray("bookmarks");
                        List<BookmarkManager.BookmarkItem> bList = new ArrayList<>();
                        for (int i = 0; i < bArr.length(); i++) {
                            org.json.JSONObject b = bArr.getJSONObject(i);
                            bList.add(new BookmarkManager.BookmarkItem(b.optString("title"), b.optString("url"), b.optLong("timestamp")));
                        }
                        bmAdded = bookmarkManager.importBookmarks(bList);
                    }
                    int hAdded = 0;
                    if (root.has("history")) {
                        org.json.JSONArray hArr = root.getJSONArray("history");
                        List<HistoryManager.HistoryItem> hList = new ArrayList<>();
                        for (int i = 0; i < hArr.length(); i++) {
                            org.json.JSONObject h = hArr.getJSONObject(i);
                            hList.add(new HistoryManager.HistoryItem(h.optString("title"), h.optString("url"), h.optLong("timestamp")));
                        }
                        hAdded = historyManager.importHistory(hList);
                    }
                    refreshDashboardData();
                    Toast.makeText(this, "Restored " + bmAdded + " bookmarks and " + hAdded + " history items!", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(this, "Backup restore failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    // ==========================================
    // Back Key Navigation Handling
    // ==========================================
    @Override
    public void onBackPressed() {
        if (findBar.getVisibility() == View.VISIBLE) {
            findBar.setVisibility(View.GONE);
            BrowserTab activeTab = tabManager.getActiveTab();
            if (activeTab != null && activeTab.getWebView() != null) {
                activeTab.getWebView().clearMatches();
            }
            return;
        }

        if (devtoolsContainer.getVisibility() == View.VISIBLE) {
            devtoolsContainer.setVisibility(View.GONE);
            return;
        }

        BrowserTab activeTab = tabManager.getActiveTab();
        if (webviewContainer.getVisibility() == View.VISIBLE && activeTab != null && activeTab.getWebView() != null) {
            WebView wv = activeTab.getWebView();
            if (wv.canGoBack()) {
                WebBackForwardList history = wv.copyBackForwardList();
                int currentIndex = history.getCurrentIndex();
                if (currentIndex > 0) {
                    WebHistoryItem prevItem = history.getItemAtIndex(currentIndex - 1);
                    String prevUrl = prevItem != null ? prevItem.getUrl() : "";
                    if (prevUrl == null || prevUrl.equals("about:blank") || prevUrl.equalsIgnoreCase(UrlUtils.DASHBOARD_URL)) {
                        showDashboard();
                        return;
                    }
                }
                wv.goBack();
                return;
            } else {
                // Return to Native Dashboard
                showDashboard();
                return;
            }
        }

        super.onBackPressed();
    }

    // ==========================================
    // Recycler Adapters: Tabs, Bookmarks, History
    // ==========================================
    interface TabItemClickListener {
        void onTabClick(BrowserTab tab, int position, boolean isClose);
    }

    private static class TabsAdapter extends RecyclerView.Adapter<TabsAdapter.TabViewHolder> {
        private final List<BrowserTab> tabs;
        private final TabItemClickListener listener;

        public TabsAdapter(List<BrowserTab> tabs, TabItemClickListener listener) {
            this.tabs = tabs;
            this.listener = listener;
        }

        @NonNull
        @Override
        public TabViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_tab_card, parent, false);
            return new TabViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull TabViewHolder holder, int position) {
            BrowserTab tab = tabs.get(position);
            holder.title.setText(tab.getCurrentTitle());
            holder.url.setText(tab.getCurrentUrl());
            holder.itemView.setOnClickListener(v -> listener.onTabClick(tab, holder.getAdapterPosition(), false));
            holder.btnClose.setOnClickListener(v -> listener.onTabClick(tab, holder.getAdapterPosition(), true));
        }

        @Override public int getItemCount() { return tabs.size(); }

        static class TabViewHolder extends RecyclerView.ViewHolder {
            TextView title, url;
            ImageButton btnClose;
            TabViewHolder(View v) {
                super(v);
                title = v.findViewById(R.id.tab_title);
                url = v.findViewById(R.id.tab_url);
                btnClose = v.findViewById(R.id.btn_close_tab);
            }
        }
    }

    interface BookmarkClickListener {
        void onBookmarkClick(BookmarkManager.BookmarkItem item, boolean isDelete);
    }

    private static class BookmarksAdapter extends RecyclerView.Adapter<BookmarksAdapter.BmViewHolder> {
        private final List<BookmarkManager.BookmarkItem> items;
        private final BookmarkClickListener listener;

        public BookmarksAdapter(List<BookmarkManager.BookmarkItem> items, BookmarkClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public BmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_bookmark, parent, false);
            return new BmViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull BmViewHolder holder, int position) {
            BookmarkManager.BookmarkItem bm = items.get(position);
            holder.title.setText(bm.title);
            holder.url.setText(bm.url);
            holder.itemView.setOnClickListener(v -> listener.onBookmarkClick(bm, false));
            holder.btnDelete.setOnClickListener(v -> listener.onBookmarkClick(bm, true));
        }

        @Override public int getItemCount() { return items.size(); }

        static class BmViewHolder extends RecyclerView.ViewHolder {
            TextView title, url;
            ImageButton btnDelete;
            BmViewHolder(View v) {
                super(v);
                title = v.findViewById(R.id.bm_title);
                url = v.findViewById(R.id.bm_url);
                btnDelete = v.findViewById(R.id.btn_delete_bm);
            }
        }
    }

    interface HistoryClickListener {
        void onHistoryClick(HistoryManager.HistoryItem item);
    }

    private static class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistViewHolder> {
        private final List<HistoryManager.HistoryItem> items;
        private final HistoryClickListener listener;

        public HistoryAdapter(List<HistoryManager.HistoryItem> items, HistoryClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        @NonNull
        @Override
        public HistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
            return new HistViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull HistViewHolder holder, int position) {
            HistoryManager.HistoryItem item = items.get(position);
            holder.title.setText(item.title);
            holder.url.setText(item.url);
            holder.time.setText(FormatUtils.formatTime(item.timestamp));
            holder.itemView.setOnClickListener(v -> listener.onHistoryClick(item));
        }

        @Override public int getItemCount() { return items.size(); }

        static class HistViewHolder extends RecyclerView.ViewHolder {
            TextView title, url, time;
            HistViewHolder(View v) {
                super(v);
                title = v.findViewById(R.id.hist_title);
                url = v.findViewById(R.id.hist_url);
                time = v.findViewById(R.id.hist_time);
            }
        }
    }
}
