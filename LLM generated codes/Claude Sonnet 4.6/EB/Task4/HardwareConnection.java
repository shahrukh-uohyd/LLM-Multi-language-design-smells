/**
 * Manages a hardware connection and its internal status flag.
 * Native code is responsible for resetting the flag after a timeout.
 */
public class HardwareConnection {

    static {
        System.loadLibrary("NativeHardwareConnection");
    }

    // ── Internal state ───────────────────────────────────────────────────
    private String           deviceName;
    private ConnectionStatus status;
    private int              timeoutCount;
    private long             lastResetTimestamp;  // epoch millis; 0 = never reset

    public HardwareConnection(String deviceName) {
        this.deviceName          = deviceName;
        this.status              = ConnectionStatus.DISCONNECTED;
        this.timeoutCount        = 0;
        this.lastResetTimestamp  = 0L;
    }

    /* ── Getters (read by C++ via JNI) ─────────────────────────────── */
    public String           getDeviceName()          { return deviceName;          }
    public ConnectionStatus getStatus()              { return status;              }
    public int              getTimeoutCount()        { return timeoutCount;        }
    public long             getLastResetTimestamp()  { return lastResetTimestamp;  }

    /* ── Setters (written by C++ via JNI) ──────────────────────────── */
    public void setStatus(ConnectionStatus status)              { this.status              = status;              }
    public void setTimeoutCount(int count)                      { this.timeoutCount        = count;               }
    public void setLastResetTimestamp(long lastResetTimestamp)  { this.lastResetTimestamp  = lastResetTimestamp;  }

    /* ── Native methods ─────────────────────────────────────────────── */

    /**
     * Resets the internal status flag to DISCONNECTED after a
     * hardware timeout. Called by native C++ code.
     */
    public native void resetAfterTimeout();

    /**
     * Simulates a hardware timeout, then triggers native reset.
     */
    public native void simulateTimeoutAndReset();

    /**
     * Returns a diagnostic snapshot of the current connection state.
     */
    public native String getDiagnosticReport();

    /* ── Demo main ──────────────────────────────────────────────────── */
    public static void main(String[] args) throws InterruptedException {

        HardwareConnection conn = new HardwareConnection("USB-SERIAL-01");

        // Manually set to CONNECTED to simulate an active link
        conn.setStatus(ConnectionStatus.CONNECTED);
        System.out.println("Initial state  : " + conn.getStatus());
        System.out.println(conn.getDiagnosticReport());

        // Simulate a hardware timeout then reset via native code
        System.out.println("\n>>> Simulating hardware timeout...");
        conn.simulateTimeoutAndReset();

        System.out.println("\nState after reset : " + conn.getStatus());
        System.out.println("Timeout count     : " + conn.getTimeoutCount());
        System.out.println("Last reset (epoch): " + conn.getLastResetTimestamp());
        System.out.println(conn.getDiagnosticReport());

        // Call resetAfterTimeout() directly while already DISCONNECTED
        System.out.println("\n>>> Calling resetAfterTimeout() on already-reset connection...");
        conn.resetAfterTimeout();
        System.out.println("Status : " + conn.getStatus());
        System.out.println(conn.getDiagnosticReport());
    }
}