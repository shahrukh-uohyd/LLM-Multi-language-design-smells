package com.example.jni;

/**
 * JNI bridge that delegates DataContainer instantiation to native C++ code.
 */
public class NativeObjectFactory {

    static {
        System.loadLibrary("nativefactory"); // libnativefactory.so / nativefactory.dll
    }

    /**
     * Asks the native layer to create and return a new DataContainer instance.
     *
     * @param value The float value the native code will pass to the constructor.
     * @return      A fully constructed DataContainer object, created in C++.
     */
    public native DataContainer createDataContainer(float value);


    // --- Example usage ---
    public static void main(String[] args) {
        NativeObjectFactory factory = new NativeObjectFactory();
        DataContainer container = factory.createDataContainer(3.14f);
        System.out.println("Received from native: " + container);
        System.out.println("Value: " + container.getValue());
    }
}