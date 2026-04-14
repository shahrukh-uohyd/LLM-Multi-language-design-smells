// File: src/main/java/com/example/sensor/IndustrialSensorDriver.java
package com.example.sensor;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * JNI-based industrial sensor driver wrapper that interfaces with the native
 * 'sensor-io-x64' library for monitoring industrial hardware sensors.
 */
public class IndustrialSensorDriver {
    private static final Logger logger = Logger.getLogger(IndustrialSensorDriver.class.getName());
    
    // Flag to track initialization state
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    
    // Static block to load the native library
    static {
        try {
            // Load the native sensor I/O library
            loadNativeLibrary();
            initialized.set(true);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize native sensor driver: " + e.getMessage(), e);
            initialized.set(false);
        }
    }

    /**
     * Loads the native sensor I/O library with platform-specific handling
     */
    private static void loadNativeLibrary() throws Exception {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();
        
        String libraryExtension;
        String libraryPrefix = "lib";
        
        if (osName.contains("win")) {
            libraryExtension = ".dll";
            libraryPrefix = ""; // Windows doesn't typically use 'lib' prefix
        } else if (osName.contains("mac")) {
            libraryExtension = ".dylib";
        } else {
            libraryExtension = ".so";
        }
        
        String libraryFileName = libraryPrefix + "sensor-io-x64" + libraryExtension;
        
        try {
            // First, try to load from system path
            System.loadLibrary("sensor-io-x64");
            logger.info("Successfully loaded native sensor driver library from system path");
        } catch (UnsatisfiedLinkError e) {
            logger.log(Level.WARNING, 
                      "Native library 'sensor-io-x64' not found in system path, attempting to load from resources", e);
            
            // Try to load from resources
            loadLibraryFromResources(libraryFileName);
        }
    }

    /**
     * Attempts to load the native library from resources
     */
    private static void loadLibraryFromResources(String libraryFileName) throws IOException {
        // Create temporary file
        File tempFile = File.createTempFile("sensor-io-x64", 
                                           libraryFileName.substring(libraryFileName.lastIndexOf('.')));
        tempFile.deleteOnExit();
        
        try (InputStream is = IndustrialSensorDriver.class.getResourceAsStream("/lib/" + libraryFileName)) {
            if (is != null) {
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                
                System.load(tempFile.getAbsolutePath());
                logger.info("Successfully loaded native sensor driver library from resources: " + 
                           tempFile.getAbsolutePath());
            } else {
                logger.severe("Native library not found in resources: /lib/" + libraryFileName);
                throw new UnsatisfiedLinkError("Native library not found in resources: " + libraryFileName);
            }
        }
    }

    /**
     * Reads raw sensor data from the industrial hardware
     * @return An array of integers representing raw sensor readings
     */
    public native int[] readRawData();

    /**
     * Checks if the sensor driver is properly initialized and ready to use
     * @return true if the driver is initialized and the native library is loaded
     */
    public boolean isInitialized() {
        return initialized.get();
    }

    /**
     * Safely reads raw sensor data with error handling and validation
     * @return An array of integers representing raw sensor readings, or null if operation fails
     */
    public int[] safeReadRawData() {
        try {
            if (!initialized.get()) {
                logger.warning("Sensor driver not initialized, cannot read raw data");
                return null;
            }
            
            // Call the native method to read raw data
            int[] rawData = readRawData();
            
            // Validate the returned data
            if (rawData == null) {
                logger.warning("Native method returned null for readRawData()");
                return null;
            }
            
            // Log the number of sensor readings received
            logger.fine("Received " + rawData.length + " sensor readings from native driver");
            
            return rawData;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error reading raw sensor data", e);
            return null;
        }
    }

    /**
     * Reads raw sensor data with timeout mechanism
     * @param timeoutMs Maximum time to wait for data in milliseconds
     * @return An array of integers representing raw sensor readings, or null if timeout occurs
     */
    public int[] readRawDataWithTimeout(int timeoutMs) {
        if (!initialized.get()) {
            logger.warning("Sensor driver not initialized, cannot read raw data with timeout");
            return null;
        }
        
        // In a real implementation, this would involve more sophisticated timeout handling
        // For now, we'll just call the native method directly
        return safeReadRawData();
    }

    /**
     * Gets the version of the native sensor driver
     * @return Version string of the native driver
     */
    public native String getDriverVersion();

    /**
     * Gets the version safely with error handling
     * @return Version string or "unknown" if operation fails
     */
    public String getSafeDriverVersion() {
        try {
            if (!initialized.get()) {
                return "unknown (driver not initialized)";
            }
            
            String version = getDriverVersion();
            return version != null ? version : "unknown";
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting driver version", e);
            return "unknown";
        }
    }

    /**
     * Checks if the sensor hardware is connected and responsive
     * @return true if sensor is connected and responding
     */
    public native boolean isSensorConnected();

    /**
     * Safely checks if the sensor is connected with error handling
     * @return true if sensor is connected and responding, false if error occurs
     */
    public boolean isSensorConnectedSafely() {
        try {
            if (!initialized.get()) {
                return false;
            }
            
            return isSensorConnected();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error checking sensor connection status", e);
            return false;
        }
    }

    /**
     * Initializes the sensor driver with default settings
     * @return true if initialization was successful
     */
    public native boolean initializeDriver();

    /**
     * Safely initializes the sensor driver with error handling
     * @return true if initialization was successful
     */
    public boolean initializeDriverSafely() {
        try {
            if (!initialized.get()) {
                logger.warning("Cannot initialize driver - native library not loaded");
                return false;
            }
            
            boolean result = initializeDriver();
            if (result) {
                logger.info("Sensor driver initialized successfully");
            } else {
                logger.warning("Sensor driver initialization failed");
            }
            return result;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error initializing sensor driver", e);
            return false;
        }
    }

    /**
     * Cleans up resources used by the sensor driver
     */
    public native void cleanup();

    /**
     * Safely cleans up resources with error handling
     */
    public void cleanupSafely() {
        try {
            if (initialized.get()) {
                cleanup();
                logger.info("Sensor driver resources cleaned up");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error cleaning up sensor driver resources", e);
        }
    }

    /**
     * Gets the last error from the native driver
     * @return Error message from the native driver
     */
    public native String getLastDriverError();

    /**
     * Gets the last error safely with error handling
     * @return Error message or null if no error or operation fails
     */
    public String getLastDriverErrorSafely() {
        try {
            if (!initialized.get()) {
                return "Driver not initialized";
            }
            
            return getLastDriverError();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting last driver error", e);
            return "Error retrieving driver error: " + e.getMessage();
        }
    }

    /**
     * Reads sensor data and converts to a more usable format
     * @return Array of doubles representing converted sensor values
     */
    public double[] readConvertedData() {
        int[] rawData = safeReadRawData();
        if (rawData == null) {
            return null;
        }
        
        double[] convertedData = new double[rawData.length];
        for (int i = 0; i < rawData.length; i++) {
            // Example conversion - in real implementation, this would depend on sensor type
            convertedData[i] = rawData[i] / 1000.0; // Convert from milli-units to base units
        }
        
        return convertedData;
    }

    /**
     * Gets the initialization status of the sensor driver
     * @return true if the native library is loaded and ready for use
     */
    public static boolean isDriverReady() {
        return initialized.get();
    }

    /**
     * Finalizer to ensure cleanup happens even if not explicitly called
     */
    @Override
    protected void finalize() throws Throwable {
        try {
            cleanupSafely();
        } finally {
            super.finalize();
        }
    }
}