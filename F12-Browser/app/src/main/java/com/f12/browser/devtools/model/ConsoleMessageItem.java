package com.f12.browser.devtools.model;

public class ConsoleMessageItem {
    public enum Level { LOG, INFO, WARN, ERROR, DEBUG }

    private final Level level;
    private final String message;
    private final String source;
    private final int lineNumber;
    private final long timestamp;

    public ConsoleMessageItem(Level level, String message, String source, int lineNumber, long timestamp) {
        this.level = level;
        this.message = message;
        this.source = source;
        this.lineNumber = lineNumber;
        this.timestamp = timestamp;
    }

    public Level getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getSource() {
        return source;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
