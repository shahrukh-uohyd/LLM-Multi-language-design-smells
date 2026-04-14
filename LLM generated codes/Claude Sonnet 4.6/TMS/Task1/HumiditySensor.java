package com.hardware.sensors;

/**
 * Provides native bindings for the humidity sensor hardware.
 * Values are returned as a percentage (0.0 - 100.0).
 */
public class HumiditySensor extends SensorBase {

    static {
        System.loadLibrary("humidity_sensor"); // Loads libhumidity_sensor.so / .dll
    }

    /**
     * Initializes the humidity sensor hardware.
     * Configures the sensor's humidity oversampling and filter coefficient.
     */
    @Override
    public native void initialize();

    /**
     * Reads the current relative humidity from the sensor.
     *
     * @return current relative humidity as a percentage (0.0 to 100.0)
     */
    @Override
    public native double readValue();
}