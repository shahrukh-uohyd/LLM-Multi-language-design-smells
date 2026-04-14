import java.time.Instant;

/**
 * Immutable point-in-time snapshot of the three hardware metrics
 * collected from the native layer.
 *
 * <p>Each per-metric {@link LoadStatus} is computed on construction
 * using the thresholds defined in {@link LoadStatus}, so callers
 * can inspect individual metric severity as well as the aggregate.
 */
public final class HardwareMetrics {

    private final double     cpuTemperatureCelsius;
    private final long       hardwareInterruptCount;
    private final double     powerConsumptionWatts;
    private final Instant    sampledAt;

    // Per-metric derived statuses
    private final LoadStatus cpuTempStatus;
    private final LoadStatus irqStatus;
    private final LoadStatus powerStatus;

    /**
     * @param cpuTemperatureCelsius  CPU core temperature in °C;
     *                               must be in range [-40, 150]
     * @param hardwareInterruptCount total active hardware interrupt count;
     *                               must be ≥ 0
     * @param powerConsumptionWatts  power draw in watts; must be ≥ 0
     * @param sampledAt              instant at which the readings were taken
     * @throws IllegalArgumentException on out-of-range values
     */
    public HardwareMetrics(double  cpuTemperatureCelsius,
                           long    hardwareInterruptCount,
                           double  powerConsumptionWatts,
                           Instant sampledAt) {

        if (cpuTemperatureCelsius < -40.0 || cpuTemperatureCelsius > 150.0)
            throw new IllegalArgumentException(
                "cpuTemperatureCelsius " + cpuTemperatureCelsius
                + " is outside the physical range [-40, 150]");
        if (hardwareInterruptCount < 0)
            throw new IllegalArgumentException(
                "hardwareInterruptCount must not be negative");
        if (powerConsumptionWatts < 0.0)
            throw new IllegalArgumentException(
                "powerConsumptionWatts must not be negative");
        if (sampledAt == null)
            throw new IllegalArgumentException("sampledAt must not be null");

        this.cpuTemperatureCelsius  = cpuTemperatureCelsius;
        this.hardwareInterruptCount = hardwareInterruptCount;
        this.powerConsumptionWatts  = powerConsumptionWatts;
        this.sampledAt              = sampledAt;

        this.cpuTempStatus = LoadStatus.fromCpuTemperature(cpuTemperatureCelsius);
        this.irqStatus     = LoadStatus.fromInterruptCount(hardwareInterruptCount);
        this.powerStatus   = LoadStatus.fromPowerConsumption(powerConsumptionWatts);
    }

    /** @return CPU core temperature in degrees Celsius */
    public double getCpuTemperatureCelsius()  { return cpuTemperatureCelsius; }

    /** @return total active hardware interrupt count */
    public long   getHardwareInterruptCount() { return hardwareInterruptCount; }

    /** @return current power draw in watts */
    public double getPowerConsumptionWatts()  { return powerConsumptionWatts; }

    /** @return the instant at which these metrics were sampled */
    public Instant getSampledAt()             { return sampledAt; }

    /** @return load status derived from CPU temperature alone */
    public LoadStatus getCpuTempStatus()      { return cpuTempStatus; }

    /** @return load status derived from interrupt count alone */
    public LoadStatus getIrqStatus()          { return irqStatus; }

    /** @return load status derived from power consumption alone */
    public LoadStatus getPowerStatus()        { return powerStatus; }

    @Override
    public String toString() {
        return String.format(
            "HardwareMetrics{cpu=%.1f°C(%s), irq=%d(%s), power=%.1fW(%s), at=%s}",
            cpuTemperatureCelsius,  cpuTempStatus,
            hardwareInterruptCount, irqStatus,
            powerConsumptionWatts,  powerStatus,
            sampledAt);
    }
}