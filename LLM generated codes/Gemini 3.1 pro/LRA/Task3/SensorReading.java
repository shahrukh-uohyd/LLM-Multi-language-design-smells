package com.example.monitoring;

public class SensorReading {
    public String sensorId;
    public double measurementValue;
    public String unit;
    public long collectionTime;

    public SensorReading(String sensorId, double measurementValue, String unit, long collectionTime) {
        this.sensorId = sensorId;
        this.measurementValue = measurementValue;
        this.unit = unit;
        this.collectionTime = collectionTime;
    }

    @Override
    public String toString() {
        return String.format("Sensor[%s] Value: %.2f %s at %d", 
                sensorId, measurementValue, unit, collectionTime);
    }
}