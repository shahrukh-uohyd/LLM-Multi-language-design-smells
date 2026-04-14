// New utility class specifically for cryptographic JNI calls
public class CryptoProvider {
    static {
        // Loads the new native library for cryptography (e.g., libcrypto_native.so)
        System.loadLibrary("crypto_native");
    }

    // Native method to encrypt data using a secret key
    public native byte[] encrypt(byte[] plainText, byte[] secretKey);

    // Native method to decrypt data using a secret key
    public native byte[] decrypt(byte[] cipherText, byte[] secretKey);
}