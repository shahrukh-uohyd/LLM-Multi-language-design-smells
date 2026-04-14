package com.example.security.nativecrypto;

public final class RsaSignatureNative {

    static {
        System.loadLibrary("rsa_signature_native");
    }

    private RsaSignatureNative() {
        // utility class
    }

    /**
     * Creates an RSA digital signature for the given data.
     *
     * @param data data to sign
     * @param privateKeyEncoded RSA private key (e.g. PKCS#8)
     * @return signature bytes
     */
    public static native byte[] sign(byte[] data, byte[] privateKeyEncoded);
}
