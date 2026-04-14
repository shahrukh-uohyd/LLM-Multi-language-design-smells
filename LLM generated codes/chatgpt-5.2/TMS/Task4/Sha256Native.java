package com.example.security.nativecrypto;

public final class Sha256Native {

    static {
        System.loadLibrary("sha256_native");
    }

    private Sha256Native() {
        // utility class
    }

    /**
     * Computes a SHA-256 hash of the input data.
     *
     * @param data input bytes
     * @return 32-byte SHA-256 hash
     */
    public static native byte[] hash(byte[] data);
}
