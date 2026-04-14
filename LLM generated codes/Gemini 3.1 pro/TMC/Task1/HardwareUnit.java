public class HardwareUnit {
    
    // Load the native C++ library
    static {
        System.loadLibrary("iot_controller_native");
    }

    // Native declarations for hardware control
    private native boolean performSelfTest();
    private native void startMotor();

    /**
     * Internal business logic utilizing the native methods to ensure
     * safe startup of the physical hardware.
     */
    public void initializeAndStart() {
        System.out.println("Initiating Hardware Unit startup sequence...");
        
        // Invoking native self-test before turning on the physical motor
        boolean isSystemHealthy = performSelfTest();
        
        if (isSystemHealthy) {
            System.out.println("Self-test passed. Engaging motor...");
            // Invoking native motor control
            startMotor();
            System.out.println("Motor is running.");
        } else {
            System.err.println("CRITICAL: Hardware self-test failed! Motor start aborted.");
            // Handle fault state...
        }
    }
}