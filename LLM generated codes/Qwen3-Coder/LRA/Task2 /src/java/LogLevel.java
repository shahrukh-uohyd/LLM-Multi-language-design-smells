// src/java/LogLevel.java
public enum LogLevel {
    DEBUG(0),
    INFO(1),
    WARNING(2),
    ERROR(3),
    CRITICAL(4);
    
    private final int value;
    
    LogLevel(int value) {
        this.value = value;
    }
    
    public int getValue() {
        return value;
    }
    
    public static LogLevel fromValue(int value) {
        for (LogLevel level : LogLevel.values()) {
            if (level.getValue() == value) {
                return level;
            }
        }
        throw new IllegalArgumentException("Invalid log level value: " + value);
    }
}