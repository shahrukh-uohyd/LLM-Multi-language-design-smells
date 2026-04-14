/**
 * Combined security manager using all three native cryptographic features
 */
public class SecurityManager {
    
    private final CryptoHashGenerator hashGenerator;
    private final RSASignatureCreator signatureCreator;
    private final SecureRandomGenerator randomGenerator;
    
    public SecurityManager() {
        this.hashGenerator = new CryptoHashGenerator();
        this.signatureCreator = new RSASignatureCreator();
        this.randomGenerator = new SecureRandomGenerator();
    }
    
    /**
     * Generate SHA-256 hash for data
     * @param data Input data to hash
     * @return SHA-256 hash as byte array
     */
    public byte[] generateHash(byte[] data) {
        return hashGenerator.generateSHA256(data);
    }
    
    /**
     * Generate SHA-256 hash for string
     * @param data Input string to hash
     * @return SHA-256 hash as byte array
     */
    public byte[] generateHash(String data) {
        return hashGenerator.generateSHA256(data);
    }
    
    /**
     * Create RSA digital signature for data
     * @param data Data to sign
     * @param privateKey Private key for signing
     * @return Digital signature as byte array
     */
    public byte[] createSignature(byte[] data, byte[] privateKey) {
        return signatureCreator.createSignature(data, privateKey);
    }
    
    /**
     * Verify RSA digital signature
     * @param data Original data that was signed
     * @param signature Signature to verify
     * @param publicKey Public key for verification
     * @return true if signature is valid, false otherwise
     */
    public boolean verifySignature(byte[] data, byte[] signature, byte[] publicKey) {
        return signatureCreator.verifySignature(data, signature, publicKey);
    }
    
    /**
     * Generate cryptographically secure random bytes
     * @param length Number of random bytes to generate
     * @return Random byte array
     */
    public byte[] generateSecureRandom(int length) {
        return randomGenerator.generateSecureRandomBytes(length);
    }
    
    /**
     * Generate RSA key pair
     * @param keySize Size of the key in bits
     * @return RSAKeyPair containing public and private keys
     */
    public RSAKeyPair generateKeyPair(int keySize) {
        return signatureCreator.generateKeyPair(keySize);
    }
    
    /**
     * Sign data using generated key pair
     * @param data Data to sign
     * @param keyPair RSA key pair containing private key
     * @return Digital signature as byte array
     */
    public byte[] signWithGeneratedKeys(byte[] data, RSAKeyPair keyPair) {
        return createSignature(data, keyPair.getPrivateKey());
    }
}