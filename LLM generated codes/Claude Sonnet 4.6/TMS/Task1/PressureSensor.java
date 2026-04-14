package com.hardware.sensors;

/**
 * Provides native bindings for the pressure sensor hardware.
 * Values are returned in Pascals (Pa).
 */
public class PressureSensor extends SensorBase {

    static {
        System.loadLibrary("pressure_sensor"); // Loads libpressure_sensor.so / .dll
    }

    /**
     * Initializes the pressure sensor hardware.
     * Configures I2C/SPI bus, oversampling settings, and standby time.
     */
    @Override
    public native void initialize();

    /**
     * Reads the current pressure value from the sensor.
     *
     * @return current pressure in Pascals (Pa)
     */
    @Override
    public native double readValue();
}