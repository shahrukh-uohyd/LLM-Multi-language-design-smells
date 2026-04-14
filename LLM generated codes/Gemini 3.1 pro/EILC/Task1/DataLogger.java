public class DataLogger {
    static {
        // Load the native library, e.g., libdatalogger.so
        System.loadLibrary("datalogger"); 
    }

    /**
     * Synchronizes a batch of SensorLog objects to the native database.
     * @param logs Array of 30 SensorLog objects
     * @return true if successfully saved
     */
    public native boolean syncLogsToDatabase(SensorLog[] logs);
}