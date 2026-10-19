package com.f12.browser.browser;

import android.webkit.ConsoleMessage;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import com.f12.browser.devtools.DevToolsController;
import com.f12.browser.devtools.model.ConsoleMessageItem;

public class BrowserWebChromeClient extends WebChromeClient {
    public interface ChromeListener {
        void onProgressChanged(int progress);
        void onTitleChanged(String title);
    }

    private final BrowserTab tab;
    private final DevToolsController devToolsController;
    private final ChromeListener chromeListener;

    public BrowserWebChromeClient(BrowserTab tab, DevToolsController devToolsController, ChromeListener chromeListener) {
        this.tab = tab;
        this.devToolsController = devToolsController;
        this.chromeListener = chromeListener;
    }

    @Override
    public void onProgressChanged(WebView view, int newProgress) {
        super.onProgressChanged(view, newProgress);
        tab.setProgress(newProgress);
        if (chromeListener != null) {
            chromeListener.onProgressChanged(newProgress);
        }
    }

    @Override
    public void onReceivedTitle(WebView view, String title) {
        super.onReceivedTitle(view, title);
        tab.setCurrentTitle(title);
        if (chromeListener != null) {
            chromeListener.onTitleChanged(title);
        }
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage message) {
        if (message != null && devToolsController != null) {
            ConsoleMessageItem.Level level = ConsoleMessageItem.Level.LOG;
            switch (message.messageLevel()) {
                case ERROR:
                    level = ConsoleMessageItem.Level.ERROR;
                    break;
                case WARNING:
                    level = ConsoleMessageItem.Level.WARN;
                    break;
                case TIP:
                case LOG:
                    level = ConsoleMessageItem.Level.INFO;
                    break;
                case DEBUG:
                    level = ConsoleMessageItem.Level.DEBUG;
                    break;
            }

            ConsoleMessageItem item = new ConsoleMessageItem(
                    level,
                    message.message(),
                    message.sourceId(),
                    message.lineNumber(),
                    System.currentTimeMillis()
            );
            devToolsController.onConsoleMessage(item);
        }
        return super.onConsoleMessage(message);
    }
}
