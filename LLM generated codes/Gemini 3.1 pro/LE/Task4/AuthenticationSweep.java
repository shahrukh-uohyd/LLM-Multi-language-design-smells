package com.example.security;

public class AuthenticationSweep {

    // Load the native library containing the JNI C++ implementations
    static {
        System.loadLibrary("auth_sweep_lib");
    }

    /**
     * Performs a full authentication sweep by sequentially orchestrating
     * the underlying native security capabilities.
     * 
     * @param rawBiometricData The raw input buffer from the biometric sensor.
     * @return true if the signature was successfully transmitted to the vault, false otherwise.
     */
    public boolean performSweep(byte[] rawBiometricData) {
        if (rawBiometricData == null || rawBiometricData.length == 0) {
            throw new IllegalArgumentException("Biometric data cannot be null or empty");
        }

        long contextHandle = 0;
        try {
            // Step 1: Extract biometric minutiae from the raw data buffer.
            // This creates the native session context and returns its memory address.
            contextHandle = extractMinutiae(rawBiometricData);
            if (contextHandle == 0) {
                throw new RuntimeException("Native Error: Failed to extract biometric minutiae.");
            }

            // Step 2: Generate a cryptographic signature for the extracted features.
            generateSignature(contextHandle);

            // Step 3: Transmit that signature to the secure hardware vault.
            return transmitToVault(contextHandle);

        } finally {
            // Guaranteed Step: Securely wipe and free native C++ memory 
            // to prevent memory leaks and exposure of sensitive data.
            if (contextHandle != 0) {
                cleanupContext(contextHandle);
            }
        }
    }

    // --- Native Method Declarations ---

    // Parses raw data, extracts minutiae, allocates C++ context, and returns handle
    private native long extractMinutiae(byte[] rawData);

    // Reads the minutiae from the C++ context and generates a cryptographic signature
    private native void generateSignature(long handle);

    // Transmits the signature to the hardware vault and returns the success status
    private native boolean transmitToVault(long handle);

    // Securely erases and frees the allocated C++ memory
    private native void cleanupContext(long handle);
}