public class Device {
    private String deviceId;
    private int temperature;
    private boolean isRunning;
    private double voltage;
    private DeviceState state;

    // Load the native library
    static {
        System.loadLibrary("device_native"); // Loads libdevice_native.so on Linux/Mac or device_native.dll on Windows
    }

    public Device(String deviceId) {
        this.deviceId = deviceId;
        this.temperature = 25; // Default temperature
        this.isRunning = false;
        this.voltage = 12.0; // Default voltage
        this.state = DeviceState.IDLE;
    }

    // Native method declarations
    public native void performNativeOperation();
    public native int getCalculatedValue();
    public native boolean validateDevice();
    public native String getNativeDeviceInfo();
    public native void updateFromNative(int newTemp, double newVoltage);

    // Getters and setters
    public String getDeviceId() { return deviceId; }
    public int getTemperature() { return temperature; }
    public void setTemperature(int temperature) { this.temperature = temperature; }
    public boolean isRunning() { return isRunning; }
    public void setRunning(boolean running) { this.isRunning = running; }
    public double getVoltage() { return voltage; }
    public void setVoltage(double voltage) { this.voltage = voltage; }
    public DeviceState getState() { return state; }
    public void setState(DeviceState state) { this.state = state; }

    @Override
    public String toString() {
        return String.format("Device{id='%s', temp=%d°C, running=%s, voltage=%.2fV, state=%s}",
                deviceId, temperature, isRunning, voltage, state);
    }
}