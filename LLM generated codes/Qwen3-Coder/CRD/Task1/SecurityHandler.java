// SecurityHandler.java
import java.security.SecureRandom;

public class SecurityHandler {
    private FileSystemManager fileSystemManager;
    private SecureRandom secureRandom;

    public SecurityHandler() {
        this.fileSystemManager = new FileSystemManager();
        this.secureRandom = new SecureRandom();
    }

    /**
     * Encrypts data and saves it to a file
     */
    public boolean encryptAndSaveToFile(String filename, byte[] data, byte[] key) {
        try {
            byte[] encryptedData = fileSystemManager.encryptWithAES(data, key);
            return fileSystemManager.writeFile(filename, encryptedData);
        } catch (Exception e) {
            System.err.println("Encryption failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Loads encrypted file and decrypts the content
     */
    public byte[] loadAndDecryptFromFile(String filename, byte[] key) {
        try {
            byte[] encryptedData = fileSystemManager.readFile(filename);
            if (encryptedData == null) {
                return null;
            }
            return fileSystemManager.decryptWithAES(encryptedData, key);
        } catch (Exception e) {
            System.err.println("Decryption failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Generates a secure hash of the given data
     */
    public byte[] generateSecureHash(byte[] data) {
        return fileSystemManager.hashSHA256(data);
    }

    /**
     * Creates a random encryption key of specified length
     */
    public byte[] generateRandomKey(int keyLength) {
        byte[] key = fileSystemManager.generateKey(keyLength);
        if (key == null || key.length != keyLength) {
            // Fallback to Java SecureRandom if native method fails
            key = new byte[keyLength];
            secureRandom.nextBytes(key);
        }
        return key;
    }

    /**
     * Verifies digital signature
     */
    public boolean verifyDigitalSignature(byte[] data, byte[] signature, byte[] publicKey) {
        return fileSystemManager.verifySignature(data, signature, publicKey);
    }

    /**
     * Securely stores sensitive data with encryption
     */
    public boolean storeSensitiveData(String filename, String sensitiveData, String password) {
        try {
            // Generate key from password (in real app, use proper key derivation)
            byte[] key = deriveKeyFromPassword(password);
            
            // Encrypt the data
            byte[] dataBytes = sensitiveData.getBytes("UTF-8");
            byte[] encryptedData = fileSystemManager.encryptWithAES(dataBytes, key);
            
            // Save to file
            return fileSystemManager.writeFile(filename, encryptedData);
        } catch (Exception e) {
            System.err.println("Failed to store sensitive data: " + e.getMessage());
            return false;
        }
    }

    /**
     * Retrieves and decrypts sensitive data
     */
    public String retrieveSensitiveData(String filename, String password) {
        try {
            // Generate key from password
            byte[] key = deriveKeyFromPassword(password);
            
            // Read encrypted data
            byte[] encryptedData = fileSystemManager.readFile(filename);
            if (encryptedData == null) {
                return null;
            }
            
            // Decrypt the data
            byte[] decryptedData = fileSystemManager.decryptWithAES(encryptedData, key);
            
            return new String(decryptedData, "UTF-8");
        } catch (Exception e) {
            System.err.println("Failed to retrieve sensitive data: " + e.getMessage());
            return null;
        }
    }

    private byte[] deriveKeyFromPassword(String password) {
        // Simple key derivation - in production, use PBKDF2, bcrypt, or scrypt
        byte[] passwordBytes = password.getBytes();
        byte[] key = new byte[32]; // 256-bit key
        
        for (int i = 0; i < key.length; i++) {
            key[i] = passwordBytes[i % passwordBytes.length];
        }
        
        return key;
    }
}