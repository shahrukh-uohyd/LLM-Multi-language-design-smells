// src/java/LogAnalyzer.java
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class LogAnalyzer {
    static {
        try {
            System.loadLibrary("loganalyzer"); // Load native library
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native log analyzer library failed to load: " + e.getMessage());
        }
    }
    
    // Native method declarations
    public native int[] analyzeLogEventsBySeverity(LogEvent[] logEvents);
    public native Map<String, Integer> analyzeLogEventsByComponent(LogEvent[] logEvents);
    public native long[] analyzeLogEventsWithTimeRange(LogEvent[] logEvents, 
                                                      LocalDateTime startTime, 
                                                      LocalDateTime endTime);
    
    // Wrapper methods for easier usage
    public Map<LogLevel, Integer> getSeverityStatistics(LogEvent[] logEvents) {
        int[] severityCounts = analyzeLogEventsBySeverity(logEvents);
        Map<LogLevel, Integer> result = new HashMap<>();
        
        for (int i = 0; i < severityCounts.length && i < LogLevel.values().length; i++) {
            LogLevel level = LogLevel.fromValue(i);
            result.put(level, severityCounts[i]);
        }
        
        return result;
    }
    
    public static void main(String[] args) {
        LogAnalyzer analyzer = new LogAnalyzer();
        
        // Create sample log events (simulating batch of log events)
        LogEvent[] logEvents = new LogEvent[50];
        for (int i = 0; i < 50; i++) {
            LogLevel level = LogLevel.values()[i % LogLevel.values().length];
            logEvents[i] = new LogEvent(
                LocalDateTime.now().minusMinutes(i),
                level,
                "Component-" + (i % 5),
                "Sample log message " + i
            );
        }
        
        // Analyze by severity
        Map<LogLevel, Integer> severityStats = analyzer.getSeverityStatistics(logEvents);
        System.out.println("Severity Statistics:");
        for (Map.Entry<LogLevel, Integer> entry : severityStats.entrySet()) {
            System.out.println(entry.getKey().name() + ": " + entry.getValue());
        }
        
        // Analyze by component
        Map<String, Integer> componentStats = analyzer.analyzeLogEventsByComponent(logEvents);
        System.out.println("\nComponent Statistics:");
        for (Map.Entry<String, Integer> entry : componentStats.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        
        // Analyze with time range
        LocalDateTime start = LocalDateTime.now().minusHours(1);
        LocalDateTime end = LocalDateTime.now();
        long[] timeStats = analyzer.analyzeLogEventsWithTimeRange(logEvents, start, end);
        System.out.println("\nTime Range Analysis:");
        System.out.println("Total Events: " + timeStats[0]);
        System.out.println("Error Events: " + timeStats[1]);
        System.out.println("Critical Events: " + timeStats[2]);
    }
}