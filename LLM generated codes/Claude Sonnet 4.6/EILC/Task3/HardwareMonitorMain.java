package com.example.hwmonitor;

import com.example.hwmonitor.model.HardwareHandle;
import com.example.hwmonitor.model.ScanReport;

/**
 * Entry-point demonstrating the complete deep-scan flow.
 */
public class HardwareMonitorMain {

    public static void main(String[] args) {

        // HardwareHandle and SystemProbe both implement AutoCloseable —
        // try-with-resources guarantees the kernel context is always released.
        try (HardwareHandle handle = HardwareHandle.open();
             SystemProbe    probe  = new SystemProbe(handle)) {

            System.out.println("Hardware handle opened. Starting deep scan...\n");

            ScanReport report = probe.performDeepScan();

            System.out.println(report);

            // Act on individual metrics
            if (report.getCpuTemperatureCelsius() > 90.0f) {
                System.err.println("⚠  WARNING: CPU temperature critical!");
            }
            if (report.getFan1SpeedRpm() < 500 &&
                report.getFan1SpeedRpm() != (int) ScanReport.METRIC_UNAVAILABLE) {
                System.err.println("⚠  WARNING: Fan 1 speed dangerously low!");
            }
            if (report.getBatteryVoltage() < 10.8f &&
                report.getBatteryVoltage() != ScanReport.METRIC_UNAVAILABLE) {
                System.err.println("⚠  WARNING: Battery voltage below threshold!");
            }

        } catch (IllegalStateException e) {
            System.err.println("Failed to open hardware handle: " + e.getMessage());
        }
    }
}