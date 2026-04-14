// src/java/DeviceStatusUpdate.java
import java.time.LocalDateTime;

public class DeviceStatusUpdate {
    private String deviceId;
    private int statusCode;
    private String description;
    private LocalDateTime updateTime;
    
    public DeviceStatusUpdate(String deviceId, int statusCode, String description, LocalDateTime updateTime) {
        this.deviceId = deviceId;
        this.statusCode = statusCode;
        this.description = description;
        this.updateTime = updateTime;
    }
    
    // Getters
    public String getDeviceId() { return deviceId; }
    public int getStatusCode() { return statusCode; }
    public String getDescription() { return description; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    
    @Override
    public String toString() {
        return String.format("DeviceStatusUpdate{deviceId='%s', statusCode=%d, description='%s', updateTime=%s}", 
                           deviceId, statusCode, description, updateTime);
    }
    
    public FailureState getFailureState() {
        return FailureState.fromCode(statusCode);
    }
}