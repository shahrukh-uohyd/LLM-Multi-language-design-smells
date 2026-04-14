package com.example.monitor;

/**
 * Summarises repeated failure activity for a single device.
 *
 * <p>Constructed and populated entirely by the native C++ inspection engine
 * via JNI; the Java side only reads it.</p>
 */
public class FailureReport {

    /** Device for which repeated failures were detected */
    public String deviceId;

    /** Total number of failure-state updates seen for this device */
    public int totalFailureCount;

    /**
     * Longest unbroken run of consecutive failure-state updates.
     * A non-failure update (OK / WARNING / UNKNOWN) resets the streak.
     */
    public int maxConsecutiveFailures;

    /** Timestamp of the first failure-state update (epoch millis) */
    public long firstFailureTime;

    /** Timestamp of the most recent failure-state update (epoch millis) */
    public long lastFailureTime;

    /**
     * Status code of the most recently seen failure update.
     * Useful for distinguishing FAILURE vs CRITICAL at report time.
     */
    public int lastFailureCode;

    /** Description text from the most recent failure update */
    public String lastFailureDescription;

    /** Zero-arg constructor required by JNI (NewObject) */
    public FailureReport() {}

    @Override
    public String toString() {
        return String.format(
            "FailureReport{\n" +
            "  deviceId='%s'\n"                      +
            "  totalFailures=%d\n"                   +
            "  maxConsecutiveFailures=%d\n"           +
            "  firstFailureTime=%d\n"                +
            "  lastFailureTime=%d\n"                 +
            "  lastFailureCode=%d\n"                 +
            "  lastFailureDescription='%s'\n"        +
            "}",
            deviceId,
            totalFailureCount,
            maxConsecutiveFailures,
            firstFailureTime,
            lastFailureTime,
            lastFailureCode,
            lastFailureDescription
        );
    }
}