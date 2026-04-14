import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gathers the three hardware metrics from the native C++ layer and
 * produces a {@link HealthReport} describing the server's current
 * load status.
 *
 * <h2>Collection strategy</h2>
 * The three native reads are issued <b>concurrently</b> on a dedicated
 * fixed thread-pool so that the total wall-clock latency of
 * {@link #generate()} is bounded by the slowest single sensor read,
 * not the sum of all three.
 *
 * <h2>Timeout</h2>
 * Each metric read is given at most {@value #READ_TIMEOUT_MS} ms to
 * complete.  A sensor that exceeds the timeout causes a
 * {@link HardwareMetricException} for that specific metric.
 *
 * <h2>Thread safety</h2>
 * {@link #generate()} is stateless and safe to call concurrently from
 * multiple threads.  The internal executor is shared and must be shut
 * down by calling {@link #shutdown()} when the instance is no longer
 * needed.
 *
 * <h2>Usage</h2>
 * <pre>
 *   SystemHealthReport reporter = new SystemHealthReport();
 *   try {
 *       HealthReport report = reporter.generate();
 *       System.out.println(report.getSummary());
 *   } finally {
 *       reporter.shutdown();
 *   }
 * </pre>
 */
public final class SystemHealthReport {

    private static final Logger LOG =
            Logger.getLogger(SystemHealthReport.class.getName());

    /** Maximum time in milliseconds to wait for a single hardware metric read. */
    public static final long READ_TIMEOUT_MS = 2_000L;

    // ------------------------------------------------------------------ //
    //  Library bootstrap                                                   //
    // ------------------------------------------------------------------ //
    static {
        /*
         * Loads:
         *   libSystemHealthReport.so    (Linux)
         *   libSystemHealthReport.dylib (macOS)
         *   SystemHealthReport.dll      (Windows)
         *
         * Set -Djava.library.path=<dir> or LD_LIBRARY_PATH accordingly.
         */
        System.loadLibrary("SystemHealthReport");
    }

    // ------------------------------------------------------------------ //
    //  Native method declarations                                          //
    // ------------------------------------------------------------------ //

    /**
     * Reads the current CPU core temperature from the hardware sensor.
     *
     * <p>The native implementation accesses the platform thermal management
     * interface (e.g. {@code /sys/class/thermal} on Linux, ACPI, or an
     * IPMI/BMC call) and returns the peak temperature across all active
     * cores in degrees Celsius.
     *
     * @return CPU core temperature in °C; guaranteed to be in [-40, 150]
     * @throws HardwareMetricException (metric=CPU_TEMPERATURE) if the
     *         sensor is unavailable, returns an out-of-range value, or
     *         the underlying hardware call fails
     */
    private native double nativeReadCpuTemperature()
            throws HardwareMetricException;

    /**
     * Fetches the total number of active hardware interrupts from the
     * platform interrupt controller.
     *
     * <p>The native implementation reads the cumulative hardware interrupt
     * counter (e.g. {@code /proc/interrupts} totals on Linux, or an
     * APIC register read) and returns the aggregate count across all CPUs
     * and interrupt lines.
     *
     * @return total active hardware interrupt count; guaranteed to be ≥ 0
     * @throws HardwareMetricException (metric=HARDWARE_INTERRUPTS) if the
     *         interrupt controller is inaccessible or the read fails
     */
    private native long nativeReadHardwareInterrupts()
            throws HardwareMetricException;

    /**
     * Retrieves the current system power consumption in watts from the
     * platform power management interface.
     *
     * <p>The native implementation queries the power delivery subsystem
     * (e.g. RAPL MSR registers, IPMI power sensor, or a BMC call) and
     * returns total system draw in watts.
     *
     * @return power consumption in watts; guaranteed to be ≥ 0
     * @throws HardwareMetricException (metric=POWER_CONSUMPTION) if the
     *         power sensor is unavailable or the read fails
     */
    private native double nativeReadPowerConsumption()
            throws HardwareMetricException;

    // ------------------------------------------------------------------ //
    //  Thread pool for concurrent metric collection                        //
    // ------------------------------------------------------------------ //

    /**
     * Fixed-size pool with one thread per metric.
     * All three reads are submitted simultaneously so wall-clock latency
     * equals max(t_cpu, t_irq, t_power) rather than their sum.
     */
    private final ExecutorService executor =
            Executors.newFixedThreadPool(3, r -> {
                Thread t = new Thread(r, "hw-metric-reader");
                t.setDaemon(true); // do not prevent JVM shutdown
                return t;
            });

    // ------------------------------------------------------------------ //
    //  Public API                                                          //
    // ------------------------------------------------------------------ //

    /**
     * Generates a complete {@link HealthReport} by concurrently collecting
     * all three hardware metrics and classifying the aggregate load status.
     *
     * <p>All three native reads are issued in parallel. If any read fails
     * or times out a {@link HardwareMetricException} is thrown identifying
     * exactly which metric caused the failure.
     *
     * @return a fully populated {@link HealthReport}; never {@code null}
     * @throws HardwareMetricException if any native hardware read fails
     *         or exceeds {@value #READ_TIMEOUT_MS} ms
     */
    public HealthReport generate() throws HardwareMetricException {

        LOG.log(Level.INFO, "SystemHealthReport: starting concurrent metric collection");

        // ── Submit all three reads concurrently ──────────────────────── //
        Future<Double> futureCpuTemp = executor.submit(
                (Callable<Double>) this::nativeReadCpuTemperature);

        Future<Long> futureIrqCount = executor.submit(
                (Callable<Long>) this::nativeReadHardwareInterrupts);

        Future<Double> futurePower = executor.submit(
                (Callable<Double>) this::nativeReadPowerConsumption);

        Instant sampledAt = Instant.now();

        // ── Collect results with per-metric timeout ───────────────────── //
        double cpuTemp   = awaitMetric(futureCpuTemp,
                                       HardwareMetricException.Metric.CPU_TEMPERATURE);
        long   irqCount  = awaitMetric(futureIrqCount,
                                       HardwareMetricException.Metric.HARDWARE_INTERRUPTS);
        double powerWatts = awaitMetric(futurePower,
                                        HardwareMetricException.Metric.POWER_CONSUMPTION);

        LOG.log(Level.FINE,
                "Metrics collected — CPU: {0}°C, IRQ: {1}, Power: {2}W",
                new Object[]{cpuTemp, irqCount, powerWatts});

        // ── Build snapshot and classify ───────────────────────────────── //
        HardwareMetrics metrics = new HardwareMetrics(
                cpuTemp, irqCount, powerWatts, sampledAt);

        LoadStatus overall = LoadStatus.aggregate(
                metrics.getCpuTempStatus(),
                metrics.getIrqStatus(),
                metrics.getPowerStatus());

        HealthReport report = new HealthReport(metrics, overall, Instant.now());

        LOG.log(Level.INFO, "SystemHealthReport complete: {0}", report.getSummary());

        return report;
    }

    /**
     * Shuts down the internal metric-collection thread pool.
     * Call this when the {@link SystemHealthReport} instance is no longer needed.
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS))
                executor.shutdownNow();
        } catch (InterruptedException ie) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // ------------------------------------------------------------------ //
    //  Private helpers                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Blocks until the {@link Future} resolves (up to {@value #READ_TIMEOUT_MS} ms)
     * and unwraps the result.  Any failure is wrapped in a
     * {@link HardwareMetricException} that identifies {@code metric}.
     */
    @SuppressWarnings("unchecked")
    private <T> T awaitMetric(Future<T> future,
                               HardwareMetricException.Metric metric)
            throws HardwareMetricException {
        try {
            return future.get(READ_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new HardwareMetricException(metric,
                "Hardware read timed out after " + READ_TIMEOUT_MS + " ms", ex);

        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof HardwareMetricException)
                throw (HardwareMetricException) cause;
            throw new HardwareMetricException(metric,
                "Native hardware read threw an unexpected exception: "
                + cause.getMessage(), cause);

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new HardwareMetricException(metric,
                "Metric collection thread was interrupted", ex);
        }
    }

    // ------------------------------------------------------------------ //
    //  Smoke-test entry point                                              //
    // ------------------------------------------------------------------ //

    /**
     * Minimal integration smoke test.
     * Run with: {@code java -Djava.library.path=. SystemHealthReport}
     */
    public static void main(String[] args) {
        SystemHealthReport reporter = new SystemHealthReport();
        try {
            HealthReport report = reporter.generate();
            System.out.println(report.getSummary());
            System.out.println();
            System.out.printf("  CPU Temperature : %.1f °C  → %s%n",
                    report.getMetrics().getCpuTemperatureCelsius(),
                    report.getMetrics().getCpuTempStatus());
            System.out.printf("  HW Interrupts   : %,d     → %s%n",
                    report.getMetrics().getHardwareInterruptCount(),
                    report.getMetrics().getIrqStatus());
            System.out.printf("  Power Draw      : %.1f W   → %s%n",
                    report.getMetrics().getPowerConsumptionWatts(),
                    report.getMetrics().getPowerStatus());
            System.out.printf("  Overall Status  : %s%n",
                    report.getOverallStatus());
        } catch (HardwareMetricException ex) {
            System.err.println("Failed to read metric " + ex.getMetric()
                    + ": " + ex.getMessage());
        } finally {
            reporter.shutdown();
        }
    }
}