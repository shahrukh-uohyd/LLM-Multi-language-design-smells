package com.example.security.nativecrypto;

public final class SecureRandomNative {

    static {
        System.loadLibrary("secure_random_native");
    }

    private SecureRandomNative() {
        // utility class
    }

    /**
     * Generates cryptographically secure random bytes.
     *
     * @param length number of random bytes to generate
     * @return random byte array
     */
    public static native byte[] generateRandomBytes(int length);
}
