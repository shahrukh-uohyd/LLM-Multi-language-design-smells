public class Device {
    // Device internal state
    private int id;
    private String name;
    private boolean isActive;

    public Device(int id, String name, boolean isActive) {
        this.id = id;
        this.name = name;
        this.isActive = isActive;
    }

    // Getters that can be accessed from C++
    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public boolean isActive() {
        return isActive;
    }

    // Declare the native method
    public native void processDeviceOperation();

    // Load the native shared library
    static {
        // This will look for libdevice_ops.so (Linux), libdevice_ops.dylib (Mac), or device_ops.dll (Windows)
        System.loadLibrary("device_ops");
    }

    public static void main(String[] args) {
        // Create a device representation in Java
        Device myDevice = new Device(1042, "ThermalSensor-X1", true);
        
        System.out.println("Java: Invoking native operations...");
        
        // Call the native C++ method
        myDevice.processDeviceOperation();
        
        System.out.println("Java: Native operations completed.");
    }
}