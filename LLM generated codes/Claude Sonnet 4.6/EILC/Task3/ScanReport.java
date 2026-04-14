package com.example.hwmonitor.model;

/**
 * Immutable snapshot of all six hardware metrics collected during a
 * single deep-scan pass.
 *
 * <p>Units:
 * <ul>
 *   <li>Temperatures — degrees Celsius (°C)</li>
 *   <li>Fan speeds    — rotations per minute (RPM)</li>
 *   <li>Voltage       — Volts (V)</li>
 * </ul>
 *
 * <p>A value of {@link #METRIC_UNAVAILABLE} ({@code -1}) means the native
 * driver could not read that sensor.
 */
public final class ScanReport {

    /** Sentinel returned by the native layer when a sensor is unreadable. */
    public static final float METRIC_UNAVAILABLE = -1.0f;

    // ── Temperature fields (°C) ───────────────────────────────────────────────
    private final float cpuTemperatureCelsius;
    private final float gpuTemperatureCelsius;
    private final float moboTemperatureCelsius;

    // ── Fan speed fields (RPM) ────────────────────────────────────────────────
    private final int fan1SpeedRpm;
    private final int fan2SpeedRpm;

    // ── Power / voltage field (V) ─────────────────────────────────────────────
    private final float batteryVoltage;

    /**
     * Constructed by {@link com.example.hwmonitor.SystemProbe#performDeepScan()}
     * after all six native reads have completed.
     */
    public ScanReport(float cpuTemperatureCelsius,
                      float gpuTemperatureCelsius,
                      float moboTemperatureCelsius,
                      int   fan1SpeedRpm,
                      int   fan2SpeedRpm,
                      float batteryVoltage) {
        this.cpuTemperatureCelsius  = cpuTemperatureCelsius;
        this.gpuTemperatureCelsius  = gpuTemperatureCelsius;
        this.moboTemperatureCelsius = moboTemperatureCelsius;
        this.fan1SpeedRpm           = fan1SpeedRpm;
        this.fan2SpeedRpm           = fan2SpeedRpm;
        this.batteryVoltage         = batteryVoltage;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /** CPU package temperature in °C, or {@link #METRIC_UNAVAILABLE}. */
    public float getCpuTemperatureCelsius()  { return cpuTemperatureCelsius;  }

    /** GPU die temperature in °C, or {@link #METRIC_UNAVAILABLE}. */
    public float getGpuTemperatureCelsius()  { return gpuTemperatureCelsius;  }

    /** Motherboard (chipset / PCH) temperature in °C, or {@link #METRIC_UNAVAILABLE}. */
    public float getMoboTemperatureCelsius() { return moboTemperatureCelsius; }

    /** Fan 1 speed in RPM, or {@code -1} if unavailable. */
    public int   getFan1SpeedRpm()           { return fan1SpeedRpm;           }

    /** Fan 2 speed in RPM, or {@code -1} if unavailable. */
    public int   getFan2SpeedRpm()           { return fan2SpeedRpm;           }

    /** Battery / CMOS voltage in Volts, or {@link #METRIC_UNAVAILABLE}. */
    public float getBatteryVoltage()         { return batteryVoltage;         }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns {@code true} if every metric was successfully read. */
    public boolean isFullyPopulated() {
        return cpuTemperatureCelsius  != METRIC_UNAVAILABLE
            && gpuTemperatureCelsius  != METRIC_UNAVAILABLE
            && moboTemperatureCelsius != METRIC_UNAVAILABLE
            && fan1SpeedRpm           != (int) METRIC_UNAVAILABLE
            && fan2SpeedRpm           != (int) METRIC_UNAVAILABLE
            && batteryVoltage         != METRIC_UNAVAILABLE;
    }

    @Override
    public String toString() {
        return String.format(
            "ScanReport{\n" +
            "  CPU  temp  : %.1f °C\n"  +
            "  GPU  temp  : %.1f °C\n"  +
            "  Mobo temp  : %.1f °C\n"  +
            "  Fan1 speed : %d RPM\n"   +
            "  Fan2 speed : %d RPM\n"   +
            "  Battery    : %.3f V\n"   +
            "}",
            cpuTemperatureCelsius,
            gpuTemperatureCelsius,
            moboTemperatureCelsius,
            fan1SpeedRpm,
            fan2SpeedRpm,
            batteryVoltage);
    }
}