/**
 * Represents the classified operational load of the server based on
 * the three hardware metrics gathered during a health report snapshot.
 *
 * <h3>Classification thresholds</h3>
 * <pre>
 *  ┌───────────┬─────────────────┬──────────────────┬──────────────────┐
 *  │  Status   │ CPU Temp (°C)   │ Interrupts/s     │ Power (W)        │
 *  ├───────────┼─────────────────┼──────────────────┼──────────────────┤
 *  │ NOMINAL   │  < 65           │ < 500,000        │ < 150            │
 *  │ ELEVATED  │  65 – 79        │ 500,000 – 999,999│ 150 – 249        │
 *  │ HIGH      │  80 – 94        │ 1,000,000–1,999,9│ 250 – 399        │
 *  │ CRITICAL  │  ≥ 95           │ ≥ 2,000,000      │ ≥ 400            │
 *  └───────────┴─────────────────┴──────────────────┴──────────────────┘
 *  The overall status is the maximum (most severe) across all metrics.
 * </pre>
 */
public enum LoadStatus {

    /** All metrics within normal operating bounds. */
    NOMINAL,

    /** One or more metrics approaching warning thresholds. */
    ELEVATED,

    /** One or more metrics exceeding warning thresholds; attention required. */
    HIGH,

    /** One or more metrics at or beyond critical thresholds; immediate action required. */
    CRITICAL;

    // ── CPU temperature thresholds (°C) ──────────────────────────────── //
    public static final double CPU_TEMP_ELEVATED = 65.0;
    public static final double CPU_TEMP_HIGH     = 80.0;
    public static final double CPU_TEMP_CRITICAL = 95.0;

    // ── Hardware interrupt count thresholds ───────────────────────────── //
    public static final long IRQ_ELEVATED = 500_000L;
    public static final long IRQ_HIGH     = 1_000_000L;
    public static final long IRQ_CRITICAL = 2_000_000L;

    // ── Power consumption thresholds (watts) ──────────────────────────── //
    public static final double POWER_ELEVATED = 150.0;
    public static final double POWER_HIGH     = 250.0;
    public static final double POWER_CRITICAL = 400.0;

    /**
     * Classifies CPU temperature into a {@link LoadStatus}.
     *
     * @param tempCelsius CPU core temperature in degrees Celsius
     * @return the appropriate {@link LoadStatus}
     */
    public static LoadStatus fromCpuTemperature(double tempCelsius) {
        if (tempCelsius >= CPU_TEMP_CRITICAL) return CRITICAL;
        if (tempCelsius >= CPU_TEMP_HIGH)     return HIGH;
        if (tempCelsius >= CPU_TEMP_ELEVATED) return ELEVATED;
        return NOMINAL;
    }

    /**
     * Classifies the active hardware interrupt count into a {@link LoadStatus}.
     *
     * @param interruptCount total active hardware interrupts
     * @return the appropriate {@link LoadStatus}
     */
    public static LoadStatus fromInterruptCount(long interruptCount) {
        if (interruptCount >= IRQ_CRITICAL) return CRITICAL;
        if (interruptCount >= IRQ_HIGH)     return HIGH;
        if (interruptCount >= IRQ_ELEVATED) return ELEVATED;
        return NOMINAL;
    }

    /**
     * Classifies power consumption into a {@link LoadStatus}.
     *
     * @param watts current power draw in watts
     * @return the appropriate {@link LoadStatus}
     */
    public static LoadStatus fromPowerConsumption(double watts) {
        if (watts >= POWER_CRITICAL) return CRITICAL;
        if (watts >= POWER_HIGH)     return HIGH;
        if (watts >= POWER_ELEVATED) return ELEVATED;
        return NOMINAL;
    }

    /**
     * Returns the most severe {@link LoadStatus} across all three
     * per-metric statuses.
     *
     * @param tempStatus      status derived from CPU temperature
     * @param irqStatus       status derived from interrupt count
     * @param powerStatus     status derived from power consumption
     * @return the worst-case overall {@link LoadStatus}
     */
    public static LoadStatus aggregate(LoadStatus tempStatus,
                                       LoadStatus irqStatus,
                                       LoadStatus powerStatus) {
        // Enum ordinal increases with severity: NOMINAL < ELEVATED < HIGH < CRITICAL
        LoadStatus worst = tempStatus;
        if (irqStatus.ordinal()   > worst.ordinal()) worst = irqStatus;
        if (powerStatus.ordinal() > worst.ordinal()) worst = powerStatus;
        return worst;
    }
}