package com.example.hardware;

public class HumiditySensor {

    static {
        System.loadLibrary("humidity_sensor_native");
    }

    // Initializes the humidity sensor hardware
    public native void init();

    // Reads the current humidity value
    public native double readValue();
}
