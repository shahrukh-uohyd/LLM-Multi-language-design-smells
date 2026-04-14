package com.example.hwmonitor.model;

/**
 * Immutable snapshot of all six hardware metrics collected during a
 * single {@link com.example.hwmonitor.SystemProbe#performDeepScan()} pass.
 *
 * <h3>Units</h3>
 * <ul>
 *   <li>Temperatures — degrees Celsius (°C)</li>
 *   <li>Fan speeds    — rotations per minute (RPM)</li>
 *   <li>Voltage       — Volts (V)</li>
 * </ul>
 *
 * <p>A value of {@link #METRIC_UNAVAILABLE} ({@code -1}) for any field means
 * the native driver could not read that sensor (absent, powered-off, or
 * permission-denied).
 */
public final class ScanReport {

    /**
     * Sentinel value returned when a sensor cannot be read.
     * Physically impossible for real temperatures, RPMs, or voltages,
     * so it safely survives the JNI {@code float}/{@code int} boundary
     * without a wrapper object.
     */
    public static final float METRIC_UNAVAILABLE     = -1.0f;
    public static final int   METRIC_UNAVAILABLE_INT = -1;

    // ── Temperatures (°C) ─────────────────────────────────────────────────────
    private final float cpuTemperatureCelsius;
    private final float gpuTemperatureCelsius;
    private final float moboTemperatureCelsius;

    // ── Fan speeds (RPM) ──────────────────────────────────────────────────────
    private final int fan1SpeedRpm;
    private final int fan2SpeedRpm;

    // ── Voltage (V) ───────────────────────────────────────────────────────────
    private final float batteryVoltage;

    /**
     * Constructed by {@link com.example.hwmonitor.SystemProbe#performDeepScan()}
     * after all six native reads complete.
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

    /** Motherboard (chipset/PCH) temperature in °C, or {@link #METRIC_UNAVAILABLE}. */
    public float getMoboTemperatureCelsius() { return moboTemperatureCelsius; }

    /** Fan 1 rotational speed in RPM, or {@link #METRIC_UNAVAILABLE_INT}. */
    public int   getFan1SpeedRpm()           { return fan1SpeedRpm;           }

    /** Fan 2 rotational speed in RPM, or {@link #METRIC_UNAVAILABLE_INT}. */
    public int   getFan2SpeedRpm()           { return fan2SpeedRpm;           }

    /** Battery or CMOS coin-cell voltage in Volts, or {@link #METRIC_UNAVAILABLE}. */
    public float getBatteryVoltage()         { return batteryVoltage;         }

    // ── Derived helpers ───────────────────────────────────────────────────────

    /** {@code true} if every one of the six sensors was read successfully. */
    public boolean isFullyPopulated() {
        return cpuTemperatureCelsius  != METRIC_UNAVAILABLE
            && gpuTemperatureCelsius  != METRIC_UNAVAILABLE
            && moboTemperatureCelsius != METRIC_UNAVAILABLE
            && fan1SpeedRpm           != METRIC_UNAVAILABLE_INT
            && fan2SpeedRpm           != METRIC_UNAVAILABLE_INT
            && batteryVoltage         != METRIC_UNAVAILABLE;
    }

    @Override
    public String toString() {
        return String.format(
            "ScanReport {\n"             +
            "  CPU  temperature : %.1f °C\n" +
            "  GPU  temperature : %.1f °C\n" +
            "  Mobo temperature : %.1f °C\n" +
            "  Fan 1 speed      : %d RPM\n"  +
            "  Fan 2 speed      : %d RPM\n"  +
            "  Battery voltage  : %.3f V\n"  +
            "}",
            cpuTemperatureCelsius,
            gpuTemperatureCelsius,
            moboTemperatureCelsius,
            fan1SpeedRpm,
            fan2SpeedRpm,
            batteryVoltage);
    }
}