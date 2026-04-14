public class HardwareConnection {
    // The internal status flag tracking if the hardware is active
    private boolean isActive;

    public HardwareConnection() {
        this.isActive = true; // Assume it starts active for this example
    }

    // Native method to handle the hardware timeout and reset the flag
    public native void resetConnectionStatus();

    // Getter to check the status from Java
    public boolean isActive() {
        return isActive;
    }

    // Load the native library
    static {
        System.loadLibrary("hw_connection");
    }

    public static void main(String[] args) {
        HardwareConnection connection = new HardwareConnection();
        
        System.out.println("Java: Before timeout, isActive = " + connection.isActive());
        
        // Simulate a hardware timeout delegating to the C++ recovery logic
        connection.resetConnectionStatus();
        
        System.out.println("Java: After timeout, isActive = " + connection.isActive());
    }
}