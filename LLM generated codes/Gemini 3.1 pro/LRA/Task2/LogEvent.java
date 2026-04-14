package com.example.log;

public class LogEvent {
    // Severity level constants
    public static final int DEBUG = 0;
    public static final int INFO = 1;
    public static final int WARN = 2;
    public static final int ERROR = 3;

    public long timestamp;
    public int severityLevel;
    public String sourceComponent;
    public String messageText;

    public LogEvent(long timestamp, int severityLevel, String sourceComponent, String messageText) {
        this.timestamp = timestamp;
        this.severityLevel = severityLevel;
        this.sourceComponent = sourceComponent;
        this.messageText = messageText;
    }
}