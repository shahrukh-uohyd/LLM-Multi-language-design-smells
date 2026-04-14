package com.hardware.sensors;

/**
 * Provides native bindings for the temperature sensor hardware.
 * Values are returned in degrees Celsius.
 */
public class TemperatureSensor extends SensorBase {

    static {
        System.loadLibrary("temperature_sensor"); // Loads libtemperature_sensor.so / .dll
    }

    /**
     * Initializes the temperature sensor hardware.
     * Sets up ADC channels, calibration registers, and interrupt lines.
     */
    @Override
    public native void initialize();

    /**
     * Reads the current temperature value from the sensor.
     *
     * @return current temperature in degrees Celsius
     */
    @Override
    public native double readValue();
}