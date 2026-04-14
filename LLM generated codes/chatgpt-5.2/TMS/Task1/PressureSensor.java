package com.example.hardware;

public class PressureSensor {

    static {
        System.loadLibrary("pressure_sensor_native");
    }

    // Initializes the pressure sensor hardware
    public native void init();

    // Reads the current pressure value
    public native double readValue();
}
