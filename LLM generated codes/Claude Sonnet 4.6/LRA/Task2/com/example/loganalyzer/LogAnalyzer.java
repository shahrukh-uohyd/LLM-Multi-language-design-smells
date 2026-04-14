package com.example.loganalyzer;

/**
 * Native bridge class.
 * Delegates batch log analysis to the native C library via JNI.
 */
public class LogAnalyzer {

    static {
        // Loads liblog_analyzer.so (Linux/macOS) or log_analyzer.dll (Windows)
        System.loadLibrary("log_analyzer");
    }

    /**
     * Analyzes a batch of log events entirely in native C code.
     *
     * @param events Array of LogEvent objects; must not be null or empty.
     * @return A populated {@link LogSummary} with computed statistics.
     * @throws IllegalArgumentException if {@code events} is null or empty.
     */
    public native LogSummary analyzeBatch(LogEvent[] events);
}