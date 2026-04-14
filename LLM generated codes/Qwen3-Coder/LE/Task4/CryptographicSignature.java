/**
 * Represents a cryptographic signature
 */
public class CryptographicSignature {
    private byte[] signatureData;
    private String algorithm;
    private long creationTime;
    private String hashFunction;
    
    public CryptographicSignature(byte[] signatureData, String algorithm, String hashFunction) {
        this.signatureData = signatureData;
        this.algorithm = algorithm;
        this.hashFunction = hashFunction;
        this.creationTime = System.currentTimeMillis();
    }
    
    public byte[] getSignatureData() {
        return signatureData;
    }
    
    public String getAlgorithm() {
        return algorithm;
    }
    
    public String getHashFunction() {
        return hashFunction;
    }
    
    public long getCreationTime() {
        return creationTime;
    }
}