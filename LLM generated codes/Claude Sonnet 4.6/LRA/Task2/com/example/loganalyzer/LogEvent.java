package com.example.loganalyzer;

/**
 * Represents a single runtime log event.
 * Instances of this class are passed as an array to native C code for analysis.
 */
public class LogEvent {

    // Severity level constants — must stay in sync with C-side enum
    public static final int SEVERITY_DEBUG   = 0;
    public static final int SEVERITY_INFO    = 1;
    public static final int SEVERITY_WARNING = 2;
    public static final int SEVERITY_ERROR   = 3;
    public static final int SEVERITY_FATAL   = 4;

    /** Unix epoch milliseconds */
    public final long   timestamp;

    /** One of the SEVERITY_* constants above */
    public final int    severity;

    /** Name of the subsystem or component that emitted the event */
    public final String sourceComponent;

    /** Human-readable log message */
    public final String message;

    public LogEvent(long timestamp, int severity, String sourceComponent, String message) {
        if (sourceComponent == null) throw new IllegalArgumentException("sourceComponent must not be null");
        if (message        == null) throw new IllegalArgumentException("message must not be null");
        this.timestamp       = timestamp;
        this.severity        = severity;
        this.sourceComponent = sourceComponent;
        this.message         = message;
    }

    @Override
    public String toString() {
        return String.format("LogEvent{ts=%d, severity=%d, component='%s', msg='%s'}",
                             timestamp, severity, sourceComponent, message);
    }
}