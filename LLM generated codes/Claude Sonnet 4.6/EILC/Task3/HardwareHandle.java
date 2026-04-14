package com.example.hwmonitor.model;

/**
 * Represents an active, open connection to the kernel hardware-monitoring
 * subsystem (e.g. ACPI / hwmon / WMI).
 *
 * <p>The {@code nativeHandle} field stores an opaque pointer (cast to {@code long})
 * returned by the native {@code openHandle()} call.  It must <em>never</em> be
 * modified from Java; all access goes through the JNI methods on
 * {@link com.example.hwmonitor.SystemProbe}.
 *
 * <p>Lifecycle:
 * <pre>
 *   HardwareHandle handle = HardwareHandle.open();
 *   // ... use via SystemProbe ...
 *   handle.close();   // releases kernel resources
 * </pre>
 */
public final class HardwareHandle implements AutoCloseable {

    static {
        System.loadLibrary("hardware_monitor_native");  // libhardware_monitor_native.so / .dll
    }

    /**
     * Opaque pointer to the native kernel context.
     * Value 0 means the handle is closed / invalid.
     */
    private long nativeHandle;

    // ── Construction ─────────────────────────────────────────────────────────

    /** Private — use {@link #open()} factory method. */
    private HardwareHandle(long nativeHandle) {
        this.nativeHandle = nativeHandle;
    }

    /**
     * Opens a connection to the hardware-monitoring kernel subsystem.
     *
     * @return a valid, open {@link HardwareHandle}
     * @throws IllegalStateException if the kernel connection cannot be established
     */
    public static HardwareHandle open() {
        long handle = nativeOpenHandle();
        if (handle == 0L) {
            throw new IllegalStateException(
                "Failed to open hardware handle — check kernel module / permissions.");
        }
        return new HardwareHandle(handle);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    /**
     * Returns the raw native pointer.
     * Only {@link com.example.hwmonitor.SystemProbe} should call this.
     */
    public long getNativeHandle() {
        assertOpen();
        return nativeHandle;
    }

    /** Returns {@code true} if the handle is still open. */
    public boolean isOpen() {
        return nativeHandle != 0L;
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    /** Releases all kernel resources. Safe to call multiple times. */
    @Override
    public void close() {
        if (nativeHandle != 0L) {
            nativeCloseHandle(nativeHandle);
            nativeHandle = 0L;
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void assertOpen() {
        if (nativeHandle == 0L) {
            throw new IllegalStateException("HardwareHandle is already closed.");
        }
    }

    // ── Native declarations ───────────────────────────────────────────────────

    /** Opens the kernel hardware-monitor context; returns opaque pointer or 0. */
    private static native long nativeOpenHandle();

    /** Releases the kernel context identified by {@code handle}. */
    private static native void nativeCloseHandle(long handle);
}