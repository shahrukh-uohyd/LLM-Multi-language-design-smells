package com.example.logs;

public class NativeLogAnalyzer {

    static {
        System.loadLibrary("loganalyzer");
    }

    /**
     * @param events array of log events
     * @param severityCount number of severity levels
     * @return int[] counts indexed by Severity.ordinal()
     */
    public static native int[] countBySeverity(
            LogEvent[] events, int severityCount);
}
