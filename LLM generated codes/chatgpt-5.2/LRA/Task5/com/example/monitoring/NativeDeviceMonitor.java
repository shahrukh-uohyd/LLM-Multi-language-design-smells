package com.example.monitoring;

public class NativeDeviceMonitor {

    static {
        System.loadLibrary("devicemonitor");
    }

    /**
     * Identifies devices that have repeated failure status codes.
     *
     * @param updates array of device status updates
     * @param failureCode status code considered a failure
     * @param minFailures minimum number of failures to flag a device
     * @return array of device IDs with repeated failures
     */
    public static native int[] findDevicesWithRepeatedFailures(
            DeviceStatusUpdate[] updates,
            int failureCode,
            int minFailures);
}
