package com.example.hardware;

public class TemperatureSensor {

    static {
        System.loadLibrary("temperature_sensor_native");
    }

    // Initializes the temperature sensor hardware
    public native void init();

    // Reads the current temperature value
    public native double readValue();
}
