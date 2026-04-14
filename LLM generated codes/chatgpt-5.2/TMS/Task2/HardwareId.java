package com.example.nativefeatures;

public final class HardwareId {

    static {
        System.loadLibrary("hardware_id_native");
    }

    private HardwareId() {
        // utility class
    }

    /**
     * Retrieves a machine-specific hardware identifier.
     *
     * @return unique hardware ID string
     */
    public static native String getHardwareId();
}
