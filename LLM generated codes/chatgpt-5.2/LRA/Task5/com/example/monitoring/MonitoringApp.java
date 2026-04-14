package com.example.monitoring;

import java.util.Arrays;

public class MonitoringApp {

    public static void main(String[] args) {

        DeviceStatusUpdate[] updates = {
            new DeviceStatusUpdate(1, 500, "Timeout", System.currentTimeMillis()),
            new DeviceStatusUpdate(2, 200, "OK", System.currentTimeMillis()),
            new DeviceStatusUpdate(1, 500, "Timeout", System.currentTimeMillis()),
            new DeviceStatusUpdate(3, 500, "Disk error", System.currentTimeMillis()),
            new DeviceStatusUpdate(1, 500, "Timeout", System.currentTimeMillis())
        };

        int[] failingDevices =
            NativeDeviceMonitor.findDevicesWithRepeatedFailures(
                updates, 500, 2);

        System.out.println("Devices with repeated failures: "
                + Arrays.toString(failingDevices));
    }
}
