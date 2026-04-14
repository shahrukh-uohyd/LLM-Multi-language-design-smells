// New utility class specifically for cryptographic JNI calls
public class CryptoEngine {
    static {
        // Loads the new native library for cryptography (e.g., libcrypto_engine.so)
        System.loadLibrary("crypto_engine");
    }

    // Native method to encrypt payload
    public native byte[] encryptPayload(byte[] plainText, byte[] secretKey);

    // Native method to decrypt payload
    public native byte[] decryptPayload(byte[] cipherText, byte[] secretKey);
}