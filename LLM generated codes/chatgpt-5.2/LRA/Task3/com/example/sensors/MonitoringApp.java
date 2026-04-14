package com.example.sensors;

public class MonitoringApp {

    public static void main(String[] args) {

        SensorReading[] readings = {
            new SensorReading(0, 72.5, "C", System.currentTimeMillis()),
            new SensorReading(1, 15.2, "bar", System.currentTimeMillis()),
            new SensorReading(0, 95.1, "C", System.currentTimeMillis())
        };

        // Thresholds indexed by sensorId
        double[] thresholds = {
            80.0,  // sensor 0
            20.0   // sensor 1
        };

        boolean[] exceeded =
            NativeSensorInspector.inspect(readings, thresholds);

        for (int i = 0; i < exceeded.length; i++) {
            System.out.println(
                "Reading " + i + " exceeded threshold: " + exceeded[i]);
        }
    }
}
