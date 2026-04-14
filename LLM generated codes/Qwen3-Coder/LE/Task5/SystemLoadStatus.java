/**
 * Enum representing different system load statuses
 */
public enum SystemLoadStatus {
    NORMAL("Normal"),
    WARNING("Warning"),
    CRITICAL("Critical");
    
    private final String displayName;
    
    SystemLoadStatus(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}