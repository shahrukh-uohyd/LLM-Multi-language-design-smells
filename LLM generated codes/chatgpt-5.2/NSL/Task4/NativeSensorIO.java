public class NativeSensorIO {

    // Load the native driver when the class is first referenced
    static {
        try {
            System.loadLibrary("sensor-io-x64");
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException(
                "Unable to load native library 'sensor-io-x64'", e);
        }
    }

    /**
     * Reads raw sensor data from the industrial hardware.
     *
     * @return an array of integers representing raw sensor values
     */
    public static native int[] readRawData();
}
