/**
 * Container class for RSA key pair
 */
public class RSAKeyPair {
    private byte[] publicKey;
    private byte[] privateKey;
    
    public RSAKeyPair(byte[] publicKey, byte[] privateKey) {
        this.publicKey = publicKey;
        this.privateKey = privateKey;
    }
    
    public byte[] getPublicKey() {
        return publicKey;
    }
    
    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }
    
    public byte[] getPrivateKey() {
        return privateKey;
    }
    
    public void setPrivateKey(byte[] privateKey) {
        this.privateKey = privateKey;
    }
    
    @Override
    public String toString() {
        return "RSAKeyPair{publicKeyLength=" + 
               (publicKey != null ? publicKey.length : 0) + 
               ", privateKeyLength=" + 
               (privateKey != null ? privateKey.length : 0) + "}";
    }
}