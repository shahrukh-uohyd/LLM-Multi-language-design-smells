package com.example.monitoring;

public class ThresholdInspector {
    
    // Load the native library (e.g., libsensorinspector.so or sensorinspector.dll)
    static {
        System.loadLibrary("sensorinspector");
    }

    /**
     * Native C++ method to filter sensor readings.
     * @param readings Array of all collected SensorReading objects.
     * @param threshold The value above which a reading is considered an anomaly.
     * @return A dynamically sized array containing only the readings that exceed the threshold.
     */
    public native SensorReading[] findExceedingReadings(SensorReading[] readings, double threshold);

    public static void main(String[] args) {
        SensorReading[] batch = new SensorReading[] {
            new SensorReading("TEMP_01", 22.5, "Celsius", System.currentTimeMillis()),
            new SensorReading("TEMP_02", 45.0, "Celsius", System.currentTimeMillis()), // Exceeds 40.0
            new SensorReading("PRESS_01", 1.2, "ATM", System.currentTimeMillis()),
            new SensorReading("TEMP_03", 41.2, "Celsius", System.currentTimeMillis()), // Exceeds 40.0
            new SensorReading("PRESS_02", 0.9, "ATM", System.currentTimeMillis())
        };

        ThresholdInspector inspector = new ThresholdInspector();
        
        // Let's assume our alert threshold is 40.0
        double threshold = 40.0;
        SensorReading[] alerts = inspector.findExceedingReadings(batch, threshold);

        System.out.println("Alerts triggered for the following readings:");
        if (alerts != null) {
            for (SensorReading alert : alerts) {
                System.out.println(alert.toString());
            }
        }
    }
}