package com.example.log;

public class LogAnalyzer {
    
    // Load the native library (e.g., libloganalyzer.so on Linux/Mac, loganalyzer.dll on Windows)
    static {
        System.loadLibrary("loganalyzer"); 
    }

    /**
     * Native method to compute statistics on a batch of log events.
     * @param events Array of LogEvent objects.
     * @return An integer array where the index corresponds to the severity level
     *         (0=DEBUG, 1=INFO, 2=WARN, 3=ERROR) and the value is the count.
     */
    public native int[] computeSeverityStatistics(LogEvent[] events);

    public static void main(String[] args) {
        // Create a mock batch of log events
        LogEvent[] batch = new LogEvent[] {
            new LogEvent(System.currentTimeMillis(), LogEvent.INFO, "AuthModule", "User logged in"),
            new LogEvent(System.currentTimeMillis(), LogEvent.ERROR, "DBModule", "Connection timeout"),
            new LogEvent(System.currentTimeMillis(), LogEvent.WARN, "CacheModule", "Cache miss"),
            new LogEvent(System.currentTimeMillis(), LogEvent.INFO, "AuthModule", "User logged out"),
            new LogEvent(System.currentTimeMillis(), LogEvent.DEBUG, "NetworkModule", "Ping received")
        };

        LogAnalyzer analyzer = new LogAnalyzer();
        int[] stats = analyzer.computeSeverityStatistics(batch);

        if (stats != null && stats.length == 4) {
            System.out.println("Log Statistics:");
            System.out.println("DEBUG: " + stats[LogEvent.DEBUG]);
            System.out.println("INFO:  " + stats[LogEvent.INFO]);
            System.out.println("WARN:  " + stats[LogEvent.WARN]);
            System.out.println("ERROR: " + stats[LogEvent.ERROR]);
        }
    }
}