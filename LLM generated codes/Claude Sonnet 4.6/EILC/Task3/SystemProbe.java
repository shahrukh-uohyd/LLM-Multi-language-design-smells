package com.example.hwmonitor;

import com.example.hwmonitor.model.HardwareHandle;
import com.example.hwmonitor.model.ScanReport;

/**
 * Hardware-monitor probe that uses a {@link HardwareHandle} to read six
 * kernel-level metrics via a JNI bridge to the native C++ driver layer.
 *
 * <h3>Native declarations</h3>
 * Each native method receives the raw {@code nativeHandle} pointer (stored as
 * {@code long}) so the C++ side can resolve the correct kernel context without
 * any additional lookup.
 *
 * <h3>Lifecycle</h3>
 * <pre>
 *   try (HardwareHandle handle = HardwareHandle.open();
 *        SystemProbe    probe  = new SystemProbe(handle)) {
 *
 *       ScanReport report = probe.performDeepScan();
 *       System.out.println(report);
 *   }
 * </pre>
 *
 * <h3>Thread safety</h3>
 * Each {@code performDeepScan()} call is independent and stateless beyond the
 * shared {@link HardwareHandle}.  External synchronisation is required if the
 * same {@link SystemProbe} instance is accessed from multiple threads.
 */
public final class SystemProbe implements AutoCloseable {

    // ── State ─────────────────────────────────────────────────────────────────

    /** The active kernel connection used for every metric read. */
    private final HardwareHandle hardwareHandle;

    /** Whether this probe has been closed. */
    private volatile boolean closed = false;

    // ── Construction ─────────────────────────────────────────────────────────

    /**
     * Creates a {@code SystemProbe} backed by the supplied (already-open)
     * {@link HardwareHandle}.
     *
     * @param hardwareHandle non-null, open handle to the kernel subsystem
     * @throws IllegalArgumentException if {@code hardwareHandle} is null or closed
     */
    public SystemProbe(HardwareHandle hardwareHandle) {
        if (hardwareHandle == null || !hardwareHandle.isOpen()) {
            throw new IllegalArgumentException(
                "hardwareHandle must be non-null and open.");
        }
        this.hardwareHandle = hardwareHandle;
    }

    // ── Primary API ───────────────────────────────────────────────────────────

    /**
     * Performs a deep scan of the hardware subsystem by reading all six
     * metrics through the native C++ driver layer.
     *
     * <p>The six readings are collected in the following order:
     * <ol>
     *   <li>CPU temperature (°C)</li>
     *   <li>GPU temperature (°C)</li>
     *   <li>Motherboard temperature (°C)</li>
     *   <li>Fan 1 speed (RPM)</li>
     *   <li>Fan 2 speed (RPM)</li>
     *   <li>Battery voltage (V)</li>
     * </ol>
     *
     * <p>If a sensor cannot be read, the native layer returns
     * {@link ScanReport#METRIC_UNAVAILABLE} ({@code -1}) for that metric;
     * the scan continues for all remaining sensors regardless.
     *
     * @return a {@link ScanReport} snapshot with all collected metrics
     * @throws IllegalStateException if this {@code SystemProbe} has been closed
     */
    public ScanReport performDeepScan() {
        assertNotClosed();

        // Retrieve the raw native pointer once — avoids repeated JNI field
        // access across the six individual metric reads.
        final long handle = hardwareHandle.getNativeHandle();

        // ── Step 1 : CPU Temperature ──────────────────────────────��───────────
        float cpuTemp = nativeReadCpuTemperature(handle);

        // ── Step 2 : GPU Temperature ──────────────────────────────────────────
        float gpuTemp = nativeReadGpuTemperature(handle);

        // ── Step 3 : Motherboard Temperature ─────────────────────────────────
        float moboTemp = nativeReadMoboTemperature(handle);

        // ── Step 4 : Fan 1 Speed ──────────────────────────────────────────────
        int fan1Rpm = nativeReadFan1Speed(handle);

        // ── Step 5 : Fan 2 Speed ──────────────────────────────────────────────
        int fan2Rpm = nativeReadFan2Speed(handle);

        // ── Step 6 : Battery Voltage ──────────────────────────────────────────
        float batteryVoltage = nativeReadBatteryVoltage(handle);

        // Assemble and return the immutable report.
        return new ScanReport(
            cpuTemp,
            gpuTemp,
            moboTemp,
            fan1Rpm,
            fan2Rpm,
            batteryVoltage
        );
    }

    // ── Native declarations ───────────────────────────────────────────────────

    /**
     * Reads the CPU package temperature from the kernel.
     *
     * @param nativeHandle opaque pointer to the open kernel context
     * @return temperature in °C, or {@code -1f} if the sensor is unavailable
     */
    private native float nativeReadCpuTemperature(long nativeHandle);

    /**
     * Reads the GPU die temperature from the kernel.
     *
     * @param nativeHandle opaque pointer to the open kernel context
     * @return temperature in °C, or {@code -1f} if the sensor is unavailable
     */
    private native float nativeReadGpuTemperature(long nativeHandle);

    /**
     * Reads the motherboard (chipset / PCH) temperature from the kernel.
     *
     * @param nativeHandle opaque pointer to the open kernel context
     * @return temperature in °C, or {@code -1f} if the sensor is unavailable
     */
    private native float nativeReadMoboTemperature(long nativeHandle);

    /**
     * Reads the rotational speed of Fan 1 from the kernel.
     *
     * @param nativeHandle opaque pointer to the open kernel context
     * @return fan speed in RPM, or {@code -1} if the sensor is unavailable
     */
    private native int nativeReadFan1Speed(long nativeHandle);

    /**
     * Reads the rotational speed of Fan 2 from the kernel.
     *
     * @param nativeHandle opaque pointer to the open kernel context
     * @return fan speed in RPM, or {@code -1} if the sensor is unavailable
     */
    private native int nativeReadFan2Speed(long nativeHandle);

    /**
     * Reads the battery (or CMOS coin-cell) voltage from the kernel.
     *
     * @param nativeHandle opaque pointer to the open kernel context
     * @return voltage in Volts, or {@code -1f} if the sensor is unavailable
     */
    private native float nativeReadBatteryVoltage(long nativeHandle);

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Closes this probe.  Does <em>not</em> close the underlying
     * {@link HardwareHandle} — the caller retains ownership of it.
     */
    @Override
    public void close() {
        closed = true;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void assertNotClosed() {
        if (closed) {
            throw new IllegalStateException("SystemProbe has been closed.");
        }
    }
}