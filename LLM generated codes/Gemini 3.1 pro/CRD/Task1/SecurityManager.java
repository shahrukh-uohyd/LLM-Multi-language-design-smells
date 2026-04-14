// The class responsible for managing high-level security operations
public class SecurityManager {
    private final CryptoProvider cryptoProvider;

    public SecurityManager() {
        // Initialize the native crypto dependency
        this.cryptoProvider = new CryptoProvider();
    }

    public byte[] secureDataForStorage(byte[] sensitiveData, byte[] secretKey) {
        System.out.println("Processing data for secure storage...");
        
        // Delegate encryption to native C++ implementation
        byte[] encryptedData = cryptoProvider.encrypt(sensitiveData, secretKey);
        
        System.out.println("Data successfully encrypted.");
        return encryptedData;
    }

    public byte[] retrieveSecureData(byte[] encryptedData, byte[] secretKey) {
        System.out.println("Decrypting secure data...");
        
        // Delegate decryption to native C++ implementation
        byte[] decryptedData = cryptoProvider.decrypt(encryptedData, secretKey);
        
        System.out.println("Data successfully decrypted.");
        return decryptedData;
    }
}