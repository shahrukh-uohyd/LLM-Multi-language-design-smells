/**
 * Native RSA digital signature creator
 */
public class RSASignatureCreator {
    
    static {
        System.loadLibrary("rsa_signature_native");
    }
    
    // Native method to create RSA signature from data
    private native byte[] nativeCreateSignature(byte[] data, byte[] privateKey);
    
    // Native method to verify RSA signature
    private native boolean nativeVerifySignature(byte[] data, byte[] signature, byte[] publicKey);
    
    // Native method to generate RSA key pair
    private native RSAKeyPair nativeGenerateKeyPair(int keySize);
    
    /**
     * Create RSA digital signature for given data
     * @param data Data to sign
     * @param privateKey Private key in byte array format
     * @return Digital signature as byte array
     * @throws IllegalArgumentException if data or private key is null
     */
    public byte[] createSignature(byte[] data, byte[] privateKey) {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        if (privateKey == null) {
            throw new IllegalArgumentException("Private key cannot be null");
        }
        return nativeCreateSignature(data, privateKey);
    }
    
    /**
     * Verify RSA digital signature
     * @param data Original data that was signed
     * @param signature Signature to verify
     * @param publicKey Public key in byte array format
     * @return true if signature is valid, false otherwise
     * @throws IllegalArgumentException if any parameter is null
     */
    public boolean verifySignature(byte[] data, byte[] signature, byte[] publicKey) {
        if (data == null || signature == null || publicKey == null) {
            throw new IllegalArgumentException("Data, signature, and public key cannot be null");
        }
        return nativeVerifySignature(data, signature, publicKey);
    }
    
    /**
     * Generate new RSA key pair
     * @param keySize Size of the key in bits (typically 2048 or 4096)
     * @return RSAKeyPair object containing public and private keys
     * @throws IllegalArgumentException if key size is invalid
     */
    public RSAKeyPair generateKeyPair(int keySize) {
        if (keySize < 1024 || keySize > 16384 || keySize % 8 != 0) {
            throw new IllegalArgumentException("Invalid key size: must be between 1024-16384 bits and multiple of 8");
        }
        return nativeGenerateKeyPair(keySize);
    }
}