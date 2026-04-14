public class Device {

    // Load the native shared library (libNativeDevice.so / NativeDevice.dll)
    static {
        System.loadLibrary("NativeDevice");
    }

    private String      deviceId;
    private DeviceState state;

    public Device(String deviceId, DeviceState state) {
        this.deviceId = deviceId;
        this.state    = state;
    }

    /* ── Getters (called from C++ via JNI) ──────────────────────────── */
    public String      getDeviceId() { return deviceId; }
    public DeviceState getState()    { return state;    }

    /* ── Native methods implemented in C++ ──────────────────────────── */

    /**
     * Performs a native diagnostic by reading device info from C++.
     * Returns a status/result string produced by the native layer.
     */
    public native String performNativeDiagnostic();

    /**
     * Sends a command to the device via native code.
     * @param command  command string (e.g. "RESET", "PING")
     * @return         true if the command was accepted
     */
    public native boolean sendNativeCommand(String command);

    /* ── Demo main ──────────────────────────────────────────────────── */
    public static void main(String[] args) {
        DeviceState state  = new DeviceState(87, true, "v2.3.1");
        Device      device = new Device("DEV-001", state);

        System.out.println("=== Native Diagnostic ===");
        String result = device.performNativeDiagnostic();
        System.out.println("Result : " + result);

        System.out.println("\n=== Send Native Command ===");
        boolean accepted = device.sendNativeCommand("PING");
        System.out.println("Accepted: " + accepted);
    }
}