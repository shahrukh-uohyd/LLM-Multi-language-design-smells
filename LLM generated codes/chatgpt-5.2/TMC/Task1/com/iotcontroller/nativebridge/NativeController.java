package com.iotcontroller.nativebridge;

public final class NativeController {

    static {
        // Loads libiotcontroller.so / iotcontroller.dll
        System.loadLibrary("iotcontroller");
    }

    private NativeController() {
        // Prevent instantiation
    }

    /* =======================
       Hardware Unit (Internal)
       ======================= */

    public static native void startMotor();

    public static native boolean performSelfTest();


    /* =======================
       Security Module
       ======================= */

    public static native byte[] encryptTransmission(byte[] payload);

    public static native boolean verifyCertificate(byte[] certificateData);

    public static native long calculatePayloadChecksum(byte[] payload);


    /* =======================
       Cloud Service
       ======================= */

    public static native boolean connectToRemoteRelay(String endpoint);

    public static native byte[] fetchConfigUpdate();

    public static native void logEventToCloud(String eventJson);
}
