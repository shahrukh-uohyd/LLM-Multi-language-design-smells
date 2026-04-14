// SensorLog.java
public class SensorLog {
    private long timestamp;
    private String sensorId;
    private double value;
    private String dataType;
    
    public SensorLog(long timestamp, String sensorId, double value, String dataType) {
        this.timestamp = timestamp;
        this.sensorId = sensorId;
        this.value = value;
        this.dataType = dataType;
    }
    
    // Getters
    public long getTimestamp() { return timestamp; }
    public String getSensorId() { return sensorId; }
    public double getValue() { return value; }
    public String getDataType() { return dataType; }
}