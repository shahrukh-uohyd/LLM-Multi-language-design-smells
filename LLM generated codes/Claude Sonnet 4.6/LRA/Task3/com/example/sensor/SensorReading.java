package com.example.sensor;

/**
 * Represents a single periodic sensor reading captured by the monitoring system.
 * Instances are passed as an array to native C++ code for threshold inspection.
 */
public class SensorReading {

    /** Unique identifier for the sensor (e.g. "TEMP_01", "PRESSURE_03") */
    public final String sensorId;

    /** The measured value (e.g. 98.6, 1013.25) */
    public final double measurementValue;

    /** Physical unit of the measurement (e.g. "Celsius", "hPa", "RPM") */
    public final String unit;

    /**
     * UTC collection time as Unix epoch milliseconds.
     * Use {@code Instant.now().toEpochMilli()} to populate this field.
     */
    public final long collectionTime;

    public SensorReading(String sensorId,
                         double measurementValue,
                         String unit,
                         long   collectionTime) {

        if (sensorId == null || sensorId.isBlank())
            throw new IllegalArgumentException("sensorId must not be null or blank");
        if (unit == null || unit.isBlank())
            throw new IllegalArgumentException("unit must not be null or blank");

        this.sensorId         = sensorId;
        this.measurementValue = measurementValue;
        this.unit             = unit;
        this.collectionTime   = collectionTime;
    }

    @Override
    public String toString() {
        return String.format("SensorReading{id='%s', value=%.4f, unit='%s', time=%d}",
                             sensorId, measurementValue, unit, collectionTime);
    }
}