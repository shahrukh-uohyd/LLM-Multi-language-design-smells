package com.app.diagnostics;

/**
 * Provides logging and diagnostic operations through native OS-level methods.
 *
 * <p>This class is NOT significantly modified — shown as architectural
 * context. Only the static initialiser block is updated to also load the
 * new crypto library so that SecureChannel can use it without a separate
 * load point.</p>
 *
 * NOTE: The crypto library is loaded in CryptoNative — this class is
 *       untouched. Shown here purely for architectural reference.
 */
public class SystemLogger {

    static {
        System.loadLibrary("system_logger"); // existing — unchanged
    }

    // ── existing native declarations (all unchanged) ──────────────────

    /** Writes a diagnostic message to the OS-level system log. */
    public native void logToSystem(int priority, String tag, String message);

    /** Returns the current monotonic clock value in nanoseconds. */
    public native long getMonotonicTimeNanos();

    /** Captures a snapshot of current process memory stats. */
    public native long[] getMemoryStats();

    /** Returns the OS-assigned thread ID of the calling thread. */
    public native long getNativeThreadId();

    /** Flushes any buffered log entries to the underlying OS sink. */
    public native void flushLogs();
}