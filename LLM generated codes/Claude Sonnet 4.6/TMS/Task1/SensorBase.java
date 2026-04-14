package com.hardware.sensors;

/**
 * Abstract base class for all hardware sensors.
 * Enforces a common contract for initialization and reading.
 */
public abstract class SensorBase {

    /**
     * Initializes the specific sensor hardware.
     * Must be called before any read operation.
     */
    public abstract native void initialize();

    /**
     * Reads the current sensor value.
     *
     * @return the current sensor reading as a double
     */
    public abstract native double readValue();

    /**
     * Loads the native shared library.
     * Subclasses can override to load different libraries.
     */
    protected void loadLibrary(String libraryName) {
        System.loadLibrary(libraryName);
    }
}