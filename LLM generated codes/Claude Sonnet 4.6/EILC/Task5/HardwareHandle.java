package com.example.hwmonitor.model;

/**
 * Represents an active, open connection to the kernel hardware-monitoring
 * subsystem (e.g. ACPI / hwmon / WMI).
 *
 * <p>The {@code nativeHandle} field stores an opaque pointer (cast to
 * {@code long}) returned by the native {@code nativeOpenHandle()} call.
 * It must <em>never</em> be modified from Java; all access goes through
 * the JNI methods declared on {@link com.example.hwmonitor.SystemProbe}.
 *
 * <h3>Lifecycle</h3>
 * <pre>
 *   HardwareHandle handle = HardwareHandle.open();
 *   // ... used exclusively by SystemProbe ...
 *   handle.close();   // releases all kernel resources
 * </pre>
 */
public final class HardwareHandle implements AutoCloseable {

    static {
        // Loads libhardware_monitor_native.so (Linux/Android) or
        // hardware_monitor_native.dll (Windows)
        System.loadLibrary("hardware_monitor_native");
    }

    /**
     * Opaque pointer to the native kernel context.
     * {@code 0} means the handle is closed or was never opened successfully.
     */
    private long nativeHandle;

    // ── Construction ──────────────────────────────────────────────────────────

    /** Private — callers must use {@link #open()}. */
    private HardwareHandle(long nativeHandle) {
        this.nativeHandle = nativeHandle;
    }

    /**
     * Opens a connection to the hardware-monitoring kernel subsystem.
     *
     * @return a valid, open {@link HardwareHandle}
     * @throws IllegalStateException if the kernel connection cannot be
     *                               established (e.g. missing permissions or
     *                               kernel module not loaded)
     */
    public static HardwareHandle open() {
        long handle = nativeOpenHandle();
        if (handle == 0L) {
            throw new IllegalStateException(
                "Failed to open HardwareHandle — verify kernel module and permissions.");
        }
        return new HardwareHandle(handle);
    }

    // ── Package-private accessor ───────────────────────────────────────────────

    /**
     * Returns the raw native pointer.
     *
     * <p>This method is intentionally package-private: only
     * {@link com.example.hwmonitor.SystemProbe} (same package) may call it.
     *
     * @throws IllegalStateException if the handle is already closed
     */
    long getNativeHandle() {
        assertOpen();
        return nativeHandle;
    }

    /** Returns {@code true} while the kernel connection is open. */
    public boolean isOpen() {
        return nativeHandle != 0L;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /**
     * Releases all kernel resources associated with this handle.
     * Safe to call multiple times; subsequent calls are no-ops.
     */
    @Override
    public void close() {
        if (nativeHandle != 0L) {
            nativeCloseHandle(nativeHandle);
            nativeHandle = 0L;
        }
    }

    // ── Internal guard ────────────────────────────────────────────────────────

    private void assertOpen() {
        if (nativeHandle == 0L) {
            throw new IllegalStateException(
                "HardwareHandle is closed — open a new one before reading metrics.");
        }
    }

    // ── Native declarations ───────────────────────────────────────────────────

    /**
     * Allocates and initialises the kernel hardware-monitor context.
     *
     * @return opaque pointer cast to {@code long}, or {@code 0} on failure
     */
    private static native long nativeOpenHandle();

    /**
     * Releases all resources held by the kernel context identified by
     * {@code handle} and frees the underlying memory.
     */
    private static native void nativeCloseHandle(long handle);
}