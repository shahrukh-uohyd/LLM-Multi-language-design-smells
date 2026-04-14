// src/java/SensorReading.java
import java.time.LocalDateTime;

public class SensorReading {
    private String sensorId;
    private double measurementValue;
    private String unit;
    private LocalDateTime collectionTime;
    
    public SensorReading(String sensorId, double measurementValue, String unit, LocalDateTime collectionTime) {
        this.sensorId = sensorId;
        this.measurementValue = measurementValue;
        this.unit = unit;
        this.collectionTime = collectionTime;
    }
    
    // Getters
    public String getSensorId() { return sensorId; }
    public double getMeasurementValue() { return measurementValue; }
    public String getUnit() { return unit; }
    public LocalDateTime getCollectionTime() { return collectionTime; }
    
    @Override
    public String toString() {
        return String.format("SensorReading{sensorId='%s', value=%.2f %s, time=%s}", 
                           sensorId, measurementValue, unit, collectionTime);
    }
}