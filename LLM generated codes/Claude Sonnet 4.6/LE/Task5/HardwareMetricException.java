/**
 * Thrown when a hardware metric cannot be read from the native layer.
 *
 * The {@link Metric} enum identifies exactly which sensor or counter
 * failed, enabling callers to degrade gracefully (e.g. report the
 * remaining healthy metrics) rather than discarding the entire snapshot.
 */
public final class HardwareMetricException extends Exception {

    /**
     * Identifies the hardware metric that could not be read.
     */
    public enum Metric {
        /** CPU core temperature sensor read failure. */
        CPU_TEMPERATURE,

        /** Hardware interrupt counter read failure. */
        HARDWARE_INTERRUPTS,

        /** Power consumption sensor read failure. */
        POWER_CONSUMPTION
    }

    private static final long serialVersionUID = 1L;

    private final Metric metric;

    /**
     * @param metric  the metric that failed (must not be {@code null})
     * @param message concise description of the failure
     */
    public HardwareMetricException(Metric metric, String message) {
        super(formatMessage(metric, message));
        this.metric = metric;
    }

    /**
     * @param metric  the metric that failed (must not be {@code null})
     * @param message concise description of the failure
     * @param cause   the underlying cause
     */
    public HardwareMetricException(Metric metric, String message, Throwable cause) {
        super(formatMessage(metric, message), cause);
        this.metric = metric;
    }

    /** @return the hardware metric that originated this exception */
    public Metric getMetric() {
        return metric;
    }

    private static String formatMessage(Metric metric, String message) {
        return "[" + (metric != null ? metric.name() : "UNKNOWN_METRIC") + "] " + message;
    }
}