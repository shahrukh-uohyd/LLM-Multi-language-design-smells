// File: src/main/java/com/example/sensor/SensorMonitorApp.java
package com.example.sensor;

import java.util.Arrays;

/**
 * Demo application showing how to use the Industrial Sensor Driver
 */
public class SensorMonitorApp {
    private IndustrialSensorDriver sensorDriver;

    public SensorMonitorApp() {
        this.sensorDriver = new IndustrialSensorDriver();
    }

    /**
     * Demonstrates basic usage of the industrial sensor driver
     */
    public void demonstrateUsage() {
        System.out.println("=== Industrial Sensor Driver Demo ===\n");
        
        // Check if the driver is ready
        if (!IndustrialSensorDriver.isDriverReady()) {
            System.out.println("Sensor driver is not ready - native library not loaded");
            return;
        }
        
        System.out.println("Driver Version: " + sensorDriver.getSafeDriverVersion());
        
        // Initialize the driver
        boolean initResult = sensorDriver.initializeDriverSafely();
        System.out.println("Driver Initialization Result: " + initResult);
        
        // Check sensor connection
        boolean isConnected = sensorDriver.isSensorConnectedSafely();
        System.out.println("Sensor Connected: " + isConnected);
        
        if (isConnected) {
            // Read raw sensor data
            System.out.println("\nReading raw sensor data...");
            int[] rawData = sensorDriver.safeReadRawData();
            
            if (rawData != null) {
                System.out.println("Raw sensor data: " + Arrays.toString(rawData));
                System.out.println("Number of sensor readings: " + rawData.length);
                
                // Convert to more usable format
                double[] convertedData = sensorDriver.readConvertedData();
                if (convertedData != null) {
                    System.out.println("Converted sensor data: " + Arrays.toString(convertedData));
                }
            } else {
                System.out.println("Failed to read raw sensor data");
                
                // Check for errors
                String lastError = sensorDriver.getLastDriverErrorSafely();
                System.out.println("Last driver error: " + lastError);
            }
        } else {
            System.out.println("Sensor is not connected - cannot read data");
        }
        
        // Cleanup resources
        sensorDriver.cleanupSafely();
        System.out.println("\nDemo completed successfully.");
    }

    /**
     * Simulates continuous monitoring of sensor data
     */
    public void continuousMonitoringDemo() {
        System.out.println("\n=== Continuous Monitoring Demo ===\n");
        
        if (!sensorDriver.isSensorConnectedSafely()) {
            System.out.println("Sensor not connected - skipping continuous monitoring");
            return;
        }
        
        System.out.println("Starting continuous monitoring (5 iterations)...");
        
        for (int i = 0; i < 5; i++) {
            int[] rawData = sensorDriver.safeReadRawData();
            if (rawData != null) {
                System.out.printf("Iteration %d: Sensor readings = %s%n", 
                                i + 1, Arrays.toString(rawData));
            } else {
                System.out.println("Iteration " + (i + 1) + ": Failed to read sensor data");
            }
            
            // Simulate delay between readings
            try {
                Thread.sleep(1000); // Wait 1 second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        System.out.println("Continuous monitoring completed.");
    }

    public static void main(String[] args) {
        System.out.println("Industrial Sensor Driver Integration Demo");
        System.out.println("=========================================\n");
        
        SensorMonitorApp app = new SensorMonitorApp();
        
        // Run basic demonstration
        app.demonstrateUsage();
        
        // Run continuous monitoring demo
        app.continuousMonitoringDemo();
        
        System.out.println("\nApplication completed successfully.");
    }
}