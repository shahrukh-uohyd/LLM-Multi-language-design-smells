public class HardwareConnection {

    // Internal status flag
    private boolean active;

    static {
        System.loadLibrary("hardware_native");
    }

    public HardwareConnection() {
        this.active = true; // connection starts active
    }

    // Native method called after hardware timeout
    public native void resetAfterTimeout();

    public boolean isActive() {
        return active;
    }

    public static void main(String[] args) {
        HardwareConnection conn = new HardwareConnection();
        System.out.println("Before timeout, active = " + conn.isActive());

        // Simulate native timeout handling
        conn.resetAfterTimeout();

        System.out.println("After timeout, active = " + conn.isActive());
    }
}
