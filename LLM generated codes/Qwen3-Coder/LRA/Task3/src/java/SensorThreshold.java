// src/java/SensorThreshold.java
public class SensorThreshold {
    private String sensorType;
    private double minThreshold;
    private double maxThreshold;
    private String unit;
    
    public SensorThreshold(String sensorType, double minThreshold, double maxThreshold, String unit) {
        this.sensorType = sensorType;
        this.minThreshold = minThreshold;
        this.maxThreshold = maxThreshold;
        this.unit = unit;
    }
    
    // Getters
    public String getSensorType() { return sensorType; }
    public double getMinThreshold() { return minThreshold; }
    public double getMaxThreshold() { return maxThreshold; }
    public String getUnit() { return unit; }
    
    @Override
    public String toString() {
        return String.format("SensorThreshold{type='%s', min=%.2f, max=%.2f %s}", 
                           sensorType, minThreshold, maxThreshold, unit);
    }
}