package com.example.loganalyzer;

/**
 * Summary statistics returned by the native C analysis function.
 * The C code constructs and populates an instance of this class via JNI.
 */
public class LogSummary {

    /** Total number of events processed */
    public int  totalEvents;

    /** Per-severity event counts (indexed by LogEvent.SEVERITY_* constants) */
    public int  debugCount;
    public int  infoCount;
    public int  warningCount;
    public int  errorCount;
    public int  fatalCount;

    /** Earliest and latest event timestamps in the batch */
    public long firstTimestamp;
    public long lastTimestamp;

    /** The source component that appears most frequently in the batch */
    public String mostActiveComponent;

    /** Number of events from the most active component */
    public int  mostActiveComponentCount;

    // Zero-arg constructor required by JNI (object is created via NewObject)
    public LogSummary() {}

    @Override
    public String toString() {
        return String.format(
            "LogSummary{\n" +
            "  totalEvents=%d\n" +
            "  DEBUG=%d  INFO=%d  WARNING=%d  ERROR=%d  FATAL=%d\n" +
            "  firstTimestamp=%d  lastTimestamp=%d\n" +
            "  mostActiveComponent='%s' (%d events)\n" +
            "}",
            totalEvents,
            debugCount, infoCount, warningCount, errorCount, fatalCount,
            firstTimestamp, lastTimestamp,
            mostActiveComponent, mostActiveComponentCount
        );
    }
}