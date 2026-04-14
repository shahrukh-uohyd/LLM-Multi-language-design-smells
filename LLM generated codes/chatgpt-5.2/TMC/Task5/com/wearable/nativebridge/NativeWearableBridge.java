package com.wearable.nativebridge;

public final class NativeWearableBridge {

    static {
        System.loadLibrary("wearable_native"); 
    }

    /* ===================== Heart Rate ===================== */

    // Reads raw PPG sensor samples (e.g., ADC values)
    public static native int[] readPpgSensor();

    // Calculates BPM from PPG samples
    public static native int calculateBpm(int[] ppgSamples);

    /* ===================== Bluetooth Sync ===================== */

    // Initializes Bluetooth stack (returns true on success)
    public static native boolean initializeBluetoothStack();

    // Returns discovered device identifiers (MAC or UUID)
    public static native String[] scanForDevices();

    // Sends encrypted data packet to a device
    public static native boolean sendDataPacket(String deviceId,
                                                 byte[] encryptedPayload);

    /* ===================== Data Privacy ===================== */

    // Generates a secure session key (platform-backed if available)
    public static native byte[] generateSecureKey();

    // Encrypts health data using provided key
    public static native byte[] encryptHealthData(byte[] plainData,
                                                   byte[] key);
}
