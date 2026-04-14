public class SensorIOWrapper {

    // The static block executes when the class is first loaded by the JVM.
    // This ensures the native manufacturer driver is loaded before any methods are called.
    static {
        // Loads libsensor-io-x64.so (Linux), libsensor-io-x64.dylib (macOS), 
        // or sensor-io-x64.dll (Windows) from the system's java.library.path
        System.loadLibrary("sensor-io-x64");
    }

    /**
     * Reads raw data directly from the connected industrial hardware sensors.
     * 
     * @return An array of integers representing the raw hardware data points.
     */
    public native int[] readRawData();

    public static void main(String[] args) {
        System.out.println("Initializing Sensor IO Wrapper...");

        try {
            // Instantiating the class triggers the static block to load the native library
            SensorIOWrapper sensor = new SensorIOWrapper();
            System.out.println("Native library 'sensor-io-x64' loaded successfully.");

            // Example workflow (commented out so the class can be compiled/run safely without the hardware)
            /*
            System.out.println("Polling hardware for data...");
            int[] data = sensor.readRawData();
            
            if (data != null && data.length > 0) {
                System.out.println("Successfully read " + data.length + " data points.");
                for (int i = 0; i < data.length; i++) {
                    System.out.println("Data[" + i + "]: " + data[i]);
                }
            } else {
                System.out.println("No data received from the sensor.");
            }
            */

        } catch (UnsatisfiedLinkError e) {
            System.err.println("CRITICAL ERROR: Failed to load native library 'sensor-io-x64'.");
            System.err.println("Ensure the manufacturer's driver file is installed and its directory " +
                               "is included in the 'java.library.path' property.");
            e.printStackTrace();
        }
    }
}