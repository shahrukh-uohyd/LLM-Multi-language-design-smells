package com.example;

public class NativeManager {
    static {
        System.loadLibrary("native_lib");
    }

    // Native method that will return the newly created DataContainer
    public native DataContainer createContainer(float initialValue);
}