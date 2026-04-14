package com.example;

public class NativeTrigger {
    static {
        System.loadLibrary("native_lib");
    }

    // A native method to kick off the C++ process
    public native void startNativeProcess();
}