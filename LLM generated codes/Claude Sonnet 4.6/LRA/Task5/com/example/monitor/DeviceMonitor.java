package com.example.monitor;

/**
 * JNI bridge that delegates device-failure analysis to native C++ code.
 */
public class DeviceMonitor {

    static {
        // Loads libdevice_monitor.so (Linux/macOS) or device_monitor.dll (Windows)
        System.loadLibrary("device_monitor");
    }

    /**
     * Analyses a batch of device status updates and identifies devices that
     * have accumulated repeated failure states.
     *
     * <p>Only devices whose total failure count meets or exceeds
     * {@code failureThreshold} are included in the returned array.</p>
     *
     * <p>The returned reports are ordered by {@code totalFailureCount}
     * descending (most-failing device first).</p>
     *
     * @param updates          Sequence of status updates to analyse;
     *                         must not be {@code null}.
     * @param failureThreshold Minimum number of failure-state updates required
     *                         for a device to appear in the report;
     *                         must be {@literal >= 1}.
     * @return Array of {@link FailureReport} objects — one per qualifying device.
     *         Returns an empty array when no devices meet the threshold.
     * @throws IllegalArgumentException if {@code updates} is {@code null}
     *                                  or {@code failureThreshold < 1}.
     */
    public native FailureReport[] analyzeUpdates(StatusUpdate[] updates,
                                                 int            failureThreshold);
}