package com.example;

public class NativeModule {
    static {
        System.loadLibrary("native_controller_lib");
    }

    // Native method to kick off the C++ module's lifecycle
    public native void initializeAndRun();

    public static void main(String[] args) {
        NativeModule module = new NativeModule();
        module.initializeAndRun();
    }
}