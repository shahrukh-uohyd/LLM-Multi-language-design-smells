import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeUnit;

public class HardwareConnection {
    private volatile boolean isConnected;
    private String connectionId;
    private long lastActivityTime;
    private AtomicBoolean isResetting = new AtomicBoolean(false);
    private String deviceAddress;
    private int timeoutDurationMs;

    // Load the native library
    static {
        System.loadLibrary("hardware_connection");
    }

    public HardwareConnection(String connectionId, String deviceAddress, int timeoutMs) {
        this.connectionId = connectionId;
        this.deviceAddress = deviceAddress;
        this.timeoutDurationMs = timeoutMs;
        this.isConnected = false; // Default state
        this.lastActivityTime = System.currentTimeMillis();
    }

    // Native method to reset the connection status
    public native void resetConnectionStatus();

    // Native method to trigger timeout from native side
    public native boolean checkAndHandleTimeout();

    // Public methods for connection management
    public synchronized boolean connect() {
        if (isResetting.get()) {
            System.out.println("Cannot connect: Connection is being reset");
            return false;
        }
        
        // Simulate hardware connection
        System.out.println("Attempting to connect to device: " + deviceAddress);
        this.isConnected = true;
        this.lastActivityTime = System.currentTimeMillis();
        System.out.println("Connected to " + connectionId + " at " + new java.util.Date());
        return true;
    }

    public synchronized boolean disconnect() {
        if (isResetting.get()) {
            System.out.println("Cannot disconnect: Connection is being reset");
            return false;
        }
        
        this.isConnected = false;
        System.out.println("Disconnected from " + connectionId);
        return true;
    }

    // Method to simulate activity and update last activity time
    public void recordActivity() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    // Getter methods
    public boolean isConnected() {
        return isConnected;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public int getTimeoutDurationMs() {
        return timeoutDurationMs;
    }

    public boolean isResetting() {
        return isResetting.get();
    }

    @Override
    public String toString() {
        return String.format("HardwareConnection{id='%s', connected=%s, lastActivity=%d, address='%s', timeout=%dms}",
                connectionId, isConnected, lastActivityTime, deviceAddress, timeoutDurationMs);
    }

    // Example usage
    public static void main(String[] args) {
        HardwareConnection conn = new HardwareConnection("CONN-001", "192.168.1.100", 5000);
        System.out.println("Initial state: " + conn);

        // Connect
        conn.connect();
        System.out.println("After connect: " + conn);

        // Simulate some activity
        conn.recordActivity();
        System.out.println("After activity: " + conn);

        // Reset from native code
        conn.resetConnectionStatus();
        System.out.println("After native reset: " + conn);

        // Check timeout (this will also reset if needed)
        conn.checkAndHandleTimeout();
        System.out.println("After timeout check: " + conn);
    }
}