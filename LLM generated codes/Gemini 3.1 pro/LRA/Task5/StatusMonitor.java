package com.example.monitoring;

public class StatusMonitor {
    
    // Load the native C++ library
    static {
        System.loadLibrary("statusmonitor");
    }

    /**
     * Native C++ method to find devices with repeated failures.
     * @param updates Array of DeviceStatusUpdate objects representing a sequence of events.
     * @param failureCode The integer code representing a failure state.
     * @param threshold The number of times a device must fail to be flagged.
     * @return An array of Device IDs (Strings) that met or exceeded the failure threshold.
     */
    public native String[] findRepeatedFailures(DeviceStatusUpdate[] updates, int failureCode, int threshold);

    public static void main(String[] args) {
        DeviceStatusUpdate[] history = new DeviceStatusUpdate[] {
            new DeviceStatusUpdate("SERVER_A", DeviceStatusUpdate.STATUS_OK, "Ping OK", System.currentTimeMillis()),
            new DeviceStatusUpdate("SERVER_B", DeviceStatusUpdate.STATUS_FAILURE, "Connection Timeout", System.currentTimeMillis()),
            new DeviceStatusUpdate("SERVER_A", DeviceStatusUpdate.STATUS_FAILURE, "High CPU", System.currentTimeMillis()),
            new DeviceStatusUpdate("SERVER_B", DeviceStatusUpdate.STATUS_FAILURE, "Disk Full", System.currentTimeMillis()), // 2nd failure
            new DeviceStatusUpdate("SERVER_C", DeviceStatusUpdate.STATUS_OK, "Ping OK", System.currentTimeMillis())
        };

        StatusMonitor monitor = new StatusMonitor();
        
        // Find devices that have failed 2 or more times
        String[] failingDevices = monitor.findRepeatedFailures(history, DeviceStatusUpdate.STATUS_FAILURE, 2);

        System.out.println("Devices with repeated failures:");
        if (failingDevices != null) {
            for (String id : failingDevices) {
                System.out.println("- " + id);
            }
        }
    }
}