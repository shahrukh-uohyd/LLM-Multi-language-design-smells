public class SensorDeviceManager {
    
    // Load the native library containing all sensor implementations
    static {
        System.loadLibrary("sensor_device_native"); // or specify full path if needed
    }
    
    private boolean isInitialized = false;
    
    public SensorDeviceManager() {
        // Constructor can be used for basic initialization
    }
    
    /**
     * Initialize all sensors in the device
     */
    public synchronized boolean initializeAllSensors() {
        if (isInitialized) {
            return true; // Already initialized
        }
        
        boolean tempInit = initializeTemperatureSensor();
        boolean pressureInit = initializePressureSensor();
        boolean humidityInit = initializeHumiditySensor();
        
        isInitialized = tempInit && pressureInit && humidityInit;
        return isInitialized;
    }
    
    /**
     * Cleanup and close all sensors
     */
    public synchronized void cleanup() {
        if (isInitialized) {
            cleanupTemperatureSensor();
            cleanupPressureSensor();
            cleanupHumiditySensor();
            isInitialized = false;
        }
    }
    
    // Native method declarations for temperature sensor
    private native boolean initializeTemperatureSensor();
    private native double readTemperatureValue();
    private native void cleanupTemperatureSensor();
    
    // Native method declarations for pressure sensor
    private native boolean initializePressureSensor();
    private native double readPressureValue();
    private native void cleanupPressureSensor();
    
    // Native method declarations for humidity sensor
    private native boolean initializeHumiditySensor();
    private native double readHumidityValue();
    private native void cleanupHumiditySensor();
    
    /**
     * Public interface methods for accessing sensor values
     */
    public synchronized double getTemperature() throws IllegalStateException {
        if (!isInitialized) {
            throw new IllegalStateException("Sensors not initialized");
        }
        return readTemperatureValue();
    }
    
    public synchronized double getPressure() throws IllegalStateException {
        if (!isInitialized) {
            throw new IllegalStateException("Sensors not initialized");
        }
        return readPressureValue();
    }
    
    public synchronized double getHumidity() throws IllegalStateException {
        if (!isInitialized) {
            throw new IllegalStateException("Sensors not initialized");
        }
        return readHumidityValue();
    }
}