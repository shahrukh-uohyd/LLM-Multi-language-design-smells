public class DeviceManager {
    public static void main(String[] args) {
        try {
            Device device = new Device("DEV-001");
            System.out.println("Initial device state: " + device);

            // Call native methods
            device.performNativeOperation();
            int calculatedValue = device.getCalculatedValue();
            System.out.println("Calculated value from native: " + calculatedValue);

            boolean isValid = device.validateDevice();
            System.out.println("Device validation result: " + isValid);

            String nativeInfo = device.getNativeDeviceInfo();
            System.out.println("Native device info: " + nativeInfo);

            // This will update Java fields from native code
            device.updateFromNative(35, 9.5);
            System.out.println("After native update: " + device);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}