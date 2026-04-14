package com.app.monitor;

/**
 * Provides system monitoring and performance-related native operations.
 *
 * <p>This class is NOT significantly modified — shown as architectural
 * context only. The native library it loads ({@code system_monitor})
 * is fully independent from the new configuration library.</p>
 */
public class SystemMonitor {

    static {
        System.loadLibrary("system_monitor"); // existing — unchanged
    }

    // ── existing native declarations (all unchanged) ──────────────────

    /** Returns total physical memory in bytes. */
    public native long getTotalMemoryBytes();

    /** Returns currently available (free) memory in bytes. */
    public native long getAvailableMemoryBytes();

    /** Returns the system CPU usage as a percentage in [0.0, 100.0]. */
    public native double getCpuUsagePercent();

    /** Returns the number of logical CPU cores available to the JVM. */
    public native int getLogicalCpuCount();

    /** Returns the current system uptime in seconds. */
    public native long getSystemUptimeSeconds();

    /** Returns the process-level peak RSS (resident set size) in bytes. */
    public native long getPeakRssBytes();
}