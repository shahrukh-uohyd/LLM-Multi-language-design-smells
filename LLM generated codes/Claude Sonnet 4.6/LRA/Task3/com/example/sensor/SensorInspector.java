package com.example.sensor;

/**
 * JNI bridge class.
 * Delegates sensor threshold inspection entirely to the native C++ library.
 */
public class SensorInspector {

    static {
        // Loads libsensor_inspector.so (Linux/macOS) or sensor_inspector.dll (Windows)
        System.loadLibrary("sensor_inspector");
    }

    /**
     * Inspects a batch of sensor readings against a set of threshold configurations.
     *
     * <p>Only readings whose sensor ID has a matching entry in {@code thresholds}
     * are inspected. Readings with no matching threshold are silently ignored.</p>
     *
     * @param readings   Array of sensor readings to inspect; must not be {@code null}.
     * @param thresholds Array of threshold definitions; must not be {@code null}.
     * @return An array of {@link SensorAlert} objects — one per threshold violation.
     *         Returns an empty array when no violations are found.
     * @throws IllegalArgumentException if either parameter is {@code null}.
     */
    public native SensorAlert[] inspectReadings(SensorReading[]   readings,
                                                ThresholdConfig[] thresholds);
}