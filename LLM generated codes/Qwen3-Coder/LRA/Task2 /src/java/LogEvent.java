// src/java/LogEvent.java
import java.time.LocalDateTime;

public class LogEvent {
    private LocalDateTime timestamp;
    private LogLevel severityLevel;
    private String sourceComponent;
    private String message;
    
    public LogEvent(LocalDateTime timestamp, LogLevel severityLevel, String sourceComponent, String message) {
        this.timestamp = timestamp;
        this.severityLevel = severityLevel;
        this.sourceComponent = sourceComponent;
        this.message = message;
    }
    
    // Getters
    public LocalDateTime getTimestamp() { return timestamp; }
    public LogLevel getSeverityLevel() { return severityLevel; }
    public String getSourceComponent() { return sourceComponent; }
    public String getMessage() { return message; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s - %s: %s", 
                           timestamp.toString(), 
                           severityLevel.name(), 
                           sourceComponent, 
                           message);
    }
}