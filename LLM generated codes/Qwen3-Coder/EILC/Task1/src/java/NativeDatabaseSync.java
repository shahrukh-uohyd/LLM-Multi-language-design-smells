// NativeDatabaseSync.java
public class NativeDatabaseSync {
    static {
        System.loadLibrary("sensor_logger"); // Load native library
    }
    
    // Native method declarations
    public native boolean initializeDatabase(String dbPath);
    public native boolean syncLogs(SensorLog[] logs);
    public native boolean closeDatabase();
    public native int getPendingSyncCount();
    
    /**
     * Synchronize a collection of SensorLog objects with native database
     */
    public boolean synchronizeLogs(List<SensorLog> logs) {
        if (logs == null || logs.isEmpty()) {
            return true; // Nothing to sync
        }
        
        // Convert List to array
        SensorLog[] logArray = logs.toArray(new SensorLog[0]);
        
        // Call native synchronization method
        return syncLogs(logArray);
    }
}