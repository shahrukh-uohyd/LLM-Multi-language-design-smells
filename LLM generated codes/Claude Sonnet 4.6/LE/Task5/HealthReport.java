import java.time.Instant;

/**
 * Immutable system health report produced by
 * {@link SystemHealthReport#generate()}.
 *
 * <p>Bundles the raw {@link HardwareMetrics} snapshot together with
 * the aggregated {@link LoadStatus} and a human-readable summary
 * suitable for dashboard display or structured logging.
 */
public final class HealthReport {

    private final HardwareMetrics metrics;
    private final LoadStatus      overallStatus;
    private final String          summary;
    private final Instant         generatedAt;

    /**
     * @param metrics       fully populated hardware metric snapshot
     * @param overallStatus aggregated worst-case load status
     * @param generatedAt   instant the report was finalised
     */
    public HealthReport(HardwareMetrics metrics,
                        LoadStatus      overallStatus,
                        Instant         generatedAt) {
        if (metrics       == null) throw new IllegalArgumentException("metrics must not be null");
        if (overallStatus == null) throw new IllegalArgumentException("overallStatus must not be null");
        if (generatedAt   == null) throw new IllegalArgumentException("generatedAt must not be null");

        this.metrics       = metrics;
        this.overallStatus = overallStatus;
        this.generatedAt   = generatedAt;
        this.summary       = buildSummary(metrics, overallStatus);
    }

    /** @return the raw hardware metric snapshot */
    public HardwareMetrics getMetrics()       { return metrics; }

    /** @return the aggregated worst-case load status */
    public LoadStatus      getOverallStatus() { return overallStatus; }

    /** @return the instant this report was finalised */
    public Instant         getGeneratedAt()   { return generatedAt; }

    /** @return human-readable one-line summary for logging / display */
    public String          getSummary()       { return summary; }

    private static String buildSummary(HardwareMetrics m, LoadStatus overall) {
        return String.format(
            "SystemHealthReport [%s] | "
            + "CPU: %.1f°C (%s) | "
            + "IRQ: %d (%s) | "
            + "Power: %.1fW (%s)",
            overall,
            m.getCpuTemperatureCelsius(),  m.getCpuTempStatus(),
            m.getHardwareInterruptCount(), m.getIrqStatus(),
            m.getPowerConsumptionWatts(),  m.getPowerStatus());
    }

    @Override
    public String toString() {
        return "HealthReport{overall=" + overallStatus
                + ", generatedAt=" + generatedAt
                + ", " + summary + "}";
    }
}