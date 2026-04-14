package com.example.hwmonitor;

import com.example.hwmonitor.model.HardwareHandle;
import com.example.hwmonitor.model.ScanReport;

/**
 * Hardware-monitor probe that uses a {@link HardwareHandle} to read six
 * kernel-level metrics through a JNI bridge to the native C++ driver layer.
 *
 * <h3>How the HardwareHandle is used</h3>
 * <ol>
 *   <li>The caller constructs a {@code SystemProbe} with an already-open
 *       {@link HardwareHandle}.</li>
 *   <li>{@link #performDeepScan()} calls {@link HardwareHandle#getNativeHandle()}
 *       once to retrieve the raw opaque pointer ({@code long}).</li>
 *   <li>That pointer is forwarded — unchanged — to each of the six
 *       {@code private native} methods so the C++ side can resolve the
 *       correct kernel context without any additional lookup.</li>
 *   <li>The six return values are assembled into an immutable
 *       {@link ScanReport} and returned to the caller.</li>
 * </ol>
 *
 * <h3>Native declarations</h3>
 * Each native method is {@code private} and receives only the
 * {@code nativeHandle} {@code long} — the Java type system never touches
 * the raw pointer directly, preserving full encapsulation.
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
 * A {@code SystemProbe} instance is not thread-safe. If multiple threads
 * need concurrent scans, give each thread its own {@link HardwareHandle} and
 * {@code SystemProbe}.
 */
public final class SystemProbe implements AutoCloseable {

    // ── State ─────────────────────────────────────────────────────────────────

    /**
     * The active kernel connection used by every metric read in
     * {@link #performDeepScan()}.
     */
    private final HardwareHandle hardwareHandle;

    /** Guards against use after {@link #close()}. */
    private volatile boolean closed = false;

    // ── Construction ──────────────────────────────────────────────────────────

    /**
     * Creates a {@code SystemProbe} backed by the supplied, already-open
     * {@link HardwareHandle}.
     *
     * <p>The probe does <em>not</em> take ownership of the handle — the caller
     * is responsible for closing it.
     *
     * @param hardwareHandle non-null and open handle to the kernel subsystem
     * @throws IllegalArgumentException if {@code hardwareHandle} is null or
     *                                  already closed
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
     * kernel-level metrics through the native C++ driver.
     *
     * <h3>Scan sequence</h3>
     * <ol>
     *   <li>CPU temperature (°C)</li>
     *   <li>GPU temperature (°C)</li>
     *   <li>Motherboard temperature (°C)</li>
     *   <li>Fan 1 speed (RPM)</li>
     *   <li>Fan 2 speed (RPM)</li>
     *   <li>Battery voltage (V)</li>
     * </ol>
     *
     * <p>The {@link HardwareHandle} is used as follows: its raw native pointer
     * is retrieved once via {@link HardwareHandle#getNativeHandle()} and then
     * passed to each of the six native calls, so the C++ layer can address the
     * same kernel context for all reads within a single scan.
     *
     * <p>If a sensor cannot be read, the native layer returns
     * {@link ScanReport#METRIC_UNAVAILABLE} ({@code -1}) for that metric; the
     * scan continues for all remaining sensors regardless — no short-circuit.
     *
     * @return an immutable {@link ScanReport} snapshot of all six metrics
     * @throws IllegalStateException if this {@code SystemProbe} has been closed
     *                               or if the underlying handle is no longer open
     */
    public ScanReport performDeepScan() {
        assertNotClosed();

        /*
         * Retrieve the opaque kernel-context pointer from the HardwareHandle
         * exactly once.  Reusing a single local variable for all six native
         * calls is intentional:
         *   - avoids repeated getNativeHandle() / assertOpen() overhead
         *   - documents clearly that all six reads share one kernel context
         *   - makes it obvious to the C++ author which handle owns each call
         */
        final long handle = hardwareHandle.getNativeHandle();

        // ── Step 1 : CPU Temperature ──────────────────────────────────────────
        final float cpuTemperature = nativeReadCpuTemperature(handle);

        // ── Step 2 : GPU Temperature ──────────────────────────────────────────
        final float gpuTemperature = nativeReadGpuTemperature(handle);

        // ── Step 3 : Motherboard Temperature ─────────────────────────────────
        final float moboTemperature = nativeReadMoboTemperature(handle);

        // ── Step 4 : Fan 1 Speed ──────────────────────────────────────────────
        final int fan1Speed = nativeReadFan1Speed(handle);

        // ── Step 5 : Fan 2 Speed ──────────────────────────────────────────────
        final int fan2Speed = nativeReadFan2Speed(handle);

        // ── Step 6 : Battery Voltage ──────────────────────────────────────────
        final float batteryVoltage = nativeReadBatteryVoltage(handle);

        // Assemble and return the immutable snapshot.
        return new ScanReport(
            cpuTemperature,
            gpuTemperature,
            moboTemperature,
            fan1Speed,
            fan2Speed,
            batteryVoltage
        );
    }

    // ── Native declarations ───────────────────────────────────────────────────
    //
    // Each method is private: only performDeepScan() may call them.
    // The sole parameter is the opaque kernel-context pointer forwarded
    // from HardwareHandle.getNativeHandle().

    /**
     * Reads the CPU package temperature from the kernel.
     *
     * @param nativeHandle opaque pointer to the open kernel context
     *                     (retrieved from {@link HardwareHandle#getNativeHandle()})
     * @return CPU temperature in °C, or {@code -1f} if the sensor is unavailable
     */
    private native float nativeReadCpuTemperature(long nativeHandle);

    /**
     * Reads the GPU die temperature from the kernel.
     *
     * @param nativeHandle opaque pointer to the open kernel context
     * @return GPU temperature in °C, or {@code -1f} if the sensor is unavailable
     */
    private native float nativeReadGpuTemperature(long nativeHandle);

    /**
     * Reads the motherboard (chipset / PCH) temperature from the kernel.
     *
     * @param nativeHandle opaque pointer to the open kernel context
     * @return motherboard temperature in °C, or {@code -1f} if unavailable
     */
    private native float nativeReadMoboTemperature(long nativeHandle);

    /**
     * Reads the rotational speed of Fan 1 from the kernel.
     *
     * @param nativeHandle opaque pointer to the open kernel context
     * @return Fan 1 speed in RPM, or {@code -1} if the sensor is unavailable
     */
    private native int nativeReadFan1Speed(long nativeHandle);

    /**
     * Reads the rotational speed of Fan 2 from the kernel.
     *
     * @param nativeHandle opaque pointer to the open kernel context
     * @return Fan 2 speed in RPM, or {@code -1} if the sensor is unavailable
     */
    private native int nativeReadFan2Speed(long nativeHandle);

    /**
     * Reads the battery (or CMOS coin-cell) voltage from the kernel.
     *
     * @param nativeHandle opaque pointer to the open kernel context
     * @return battery voltage in Volts, or {@code -1f} if the sensor is unavailable
     */
    private native float nativeReadBatteryVoltage(long nativeHandle);

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Marks this probe as closed.
     *
     * <p>Does <em>not</em> close the underlying {@link HardwareHandle} —
     * ownership stays with the caller.  After this call,
     * {@link #performDeepScan()} will throw {@link IllegalStateException}.
     */
    @Override
    public void close() {
        closed = true;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void assertNotClosed() {
        if (closed) {
            throw new IllegalStateException(
                "SystemProbe has been closed — create a new instance to scan again.");
        }
        if (!hardwareHandle.isOpen()) {
            throw new IllegalStateException(
                "The underlying HardwareHandle has been closed externally.");
        }
    }
}