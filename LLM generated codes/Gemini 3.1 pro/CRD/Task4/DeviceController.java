// Existing class for low-level hardware communication
public class DeviceController {
    static {
        // Loads the existing native library (e.g., libdevicecontrol.so or devicecontrol.dll)
        System.loadLibrary("devicecontrol");
    }

    // Existing native methods for hardware interaction
    public native void sendCommand(int deviceId, byte[] commandPayload);
    public native int getDeviceStatus(int deviceId);
}