public class DeviceController {

    static {
        System.loadLibrary("device_native");
    }

    // Existing native methods (hardware communication)
    public native void openDevice();
    public native void closeDevice();
    public native int readRawSignal();

    // New native signal processing feature
    public native double[] processSignal(int[] rawSignal);

    // Internal device-related logic
    public void performDeviceCheck() {
        openDevice();
        int value = readRawSignal();
        System.out.println("Raw signal value: " + value);
        closeDevice();
    }
}
