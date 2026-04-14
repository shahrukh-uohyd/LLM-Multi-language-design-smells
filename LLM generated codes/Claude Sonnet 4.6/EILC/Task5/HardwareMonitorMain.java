package com.example.hwmonitor;

import com.example.hwmonitor.model.HardwareHandle;
import com.example.hwmonitor.model.ScanReport;

/**
 * Entry-point demonstrating the full deep-scan flow.
 *
 * Both {@link HardwareHandle} and {@link SystemProbe} implement
 * {@link AutoCloseable} — try-with-resources guarantees the kernel
 * context is always released, even on exception paths.
 */
public class HardwareMonitorMain {

    // Warning thresholds
    private static final float TEMP_CRITICAL_C     = 90.0f;
    private static final int   FAN_LOW_RPM          = 500;
    private static final float BATTERY_LOW_VOLTAGE  = 10.8f;

    public static void main(String[] args) {

        try (HardwareHandle handle = HardwareHandle.open();
             SystemProbe    probe  = new SystemProbe(handle)) {

            System.out.println("Hardware handle opened. Starting deep scan...\n");

            // ── performDeepScan() uses the HardwareHandle for all 6 metrics ──
            ScanReport report = probe.performDeepScan();

            System.out.println(report);
            System.out.println("Fully populated: " + report.isFullyPopulated());

            // ── Act on individual metric values ───────────────────────────────
            if (report.getCpuTemperatureCelsius() >= TEMP_CRITICAL_C) {
                System.err.println("⚠  CRITICAL: CPU temperature "
                    + report.getCpuTemperatureCelsius() + " °C — throttling risk!");
            }

            if (report.getGpuTemperatureCelsius() >= TEMP_CRITICAL_C) {
                System.err.println("⚠  CRITICAL: GPU temperature "
                    + report.getGpuTemperatureCelsius() + " °C — thermal shutdown risk!");
            }

            if (report.getFan1SpeedRpm() != ScanReport.METRIC_UNAVAILABLE_INT
                    && report.getFan1SpeedRpm() < FAN_LOW_RPM) {
                System.err.println("⚠  WARNING: Fan 1 speed critically low ("
                    + report.getFan1SpeedRpm() + " RPM).");
            }

            if (report.getFan2SpeedRpm() != ScanReport.METRIC_UNAVAILABLE_INT
                    && report.getFan2SpeedRpm() < FAN_LOW_RPM) {
                System.err.println("⚠  WARNING: Fan 2 speed critically low ("
                    + report.getFan2SpeedRpm() + " RPM).");
            }

            if (report.getBatteryVoltage() != ScanReport.METRIC_UNAVAILABLE
                    && report.getBatteryVoltage() < BATTERY_LOW_VOLTAGE) {
                System.err.printf("⚠  WARNING: Battery voltage %.3f V is below %.1f V.%n",
                    report.getBatteryVoltage(), BATTERY_LOW_VOLTAGE);
            }

        } catch (IllegalStateException e) {
            System.err.println("Fatal: could not open hardware handle — " + e.getMessage());
        }
    }
}