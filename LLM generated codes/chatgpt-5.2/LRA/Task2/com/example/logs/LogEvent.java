package com.example.logs;

public class LogEvent {
    public final long timestamp;
    public final Severity severity;
    public final String source;
    public final String message;

    public LogEvent(long timestamp, Severity severity,
                    String source, String message) {
        this.timestamp = timestamp;
        this.severity = severity;
        this.source = source;
        this.message = message;
    }
}
