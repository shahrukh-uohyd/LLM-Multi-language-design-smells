package com.example.jni;

/**
 * A simple data container class that holds a single floating-point value.
 * Instances of this class will be created by the native C++ layer via JNI.
 */
public class DataContainer {

    private final float value;

    /**
     * Constructor invoked by the native layer.
     *
     * @param value The floating-point value to store.
     */
    public DataContainer(float value) {
        this.value = value;
    }

    public float getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "DataContainer{value=" + value + "}";
    }
}