package com.f12.browser.devtools;

import android.os.Handler;
import android.os.Looper;
import android.webkit.JavascriptInterface;
import com.f12.browser.devtools.model.ConsoleMessageItem;
import com.f12.browser.devtools.model.NetworkRequestItem;
import org.json.JSONObject;

public class DevToolsBridge {
    private final DevToolsController controller;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public DevToolsBridge(DevToolsController controller) {
        this.controller = controller;
    }

    @JavascriptInterface
    public void onBridgeReady(String jsonStr) {
        mainHandler.post(() -> {
            if (controller != null) controller.onBridgeReady();
        });
    }

    @JavascriptInterface
    public void onConsoleMessage(String jsonStr) {
        try {
            JSONObject obj = new JSONObject(jsonStr);
            String levelStr = obj.optString("level", "log").toUpperCase();
            ConsoleMessageItem.Level level = ConsoleMessageItem.Level.LOG;
            if (levelStr.equals("INFO")) level = ConsoleMessageItem.Level.INFO;
            else if (levelStr.equals("WARN")) level = ConsoleMessageItem.Level.WARN;
            else if (levelStr.equals("ERROR")) level = ConsoleMessageItem.Level.ERROR;
            else if (levelStr.equals("DEBUG")) level = ConsoleMessageItem.Level.DEBUG;

            String msg = obj.optString("message", "");
            String src = obj.optString("source", "");
            int line = obj.optInt("lineno", 0);
            long ts = obj.optLong("timestamp", System.currentTimeMillis());

            ConsoleMessageItem item = new ConsoleMessageItem(level, msg, src, line, ts);
            mainHandler.post(() -> {
                if (controller != null) controller.onConsoleMessage(item);
            });
        } catch (Exception ignored) {}
    }

    @JavascriptInterface
    public void onConsoleClear(String jsonStr) {
        mainHandler.post(() -> {
            if (controller != null) controller.onConsoleClear();
        });
    }

    @JavascriptInterface
    public void onNetworkRequest(String jsonStr) {
        try {
            JSONObject obj = new JSONObject(jsonStr);
            NetworkRequestItem item = NetworkRequestItem.fromJson(obj);
            mainHandler.post(() -> {
                if (controller != null) controller.onNetworkRequest(item);
            });
        } catch (Exception ignored) {}
    }

    @JavascriptInterface
    public void onElementSelected(String jsonStr) {
        try {
            JSONObject obj = new JSONObject(jsonStr);
            String f12Id = obj.optString("f12Id", "");
            mainHandler.post(() -> {
                if (controller != null) controller.onElementSelectedFromPage(f12Id);
            });
        } catch (Exception ignored) {}
    }
}
