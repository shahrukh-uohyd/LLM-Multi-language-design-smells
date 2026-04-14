package com.example.sensors;

public class SensorReading {
    public final int sensorId;
    public final double value;
    public final String unit;
    public final long timestamp;

    public SensorReading(int sensorId, double value,
                         String unit, long timestamp) {
        this.sensorId = sensorId;
        this.value = value;
        this.unit = unit;
        this.timestamp = timestamp;
    }
}
