public class EncryptionEngine {
    static {
        System.loadLibrary("encryption_engine");
    }

    // Native method that takes SecurityKey object and initializes the encryption engine
    public native boolean initializeEncryption(SecurityKey securityKey);

    // Other native methods that use the initialized engine
    public native byte[] encryptData(byte[] plaintext);
    public native byte[] decryptData(byte[] ciphertext);
    public native boolean validateKey(SecurityKey securityKey);
    public native void cleanup();

    // Example usage method
    public static void main(String[] args) {
        try {
            // Create a test key
            SecurityKey key = SecurityKey.generateRandomKey("AES", 256);
            System.out.println("Generated key: " + key);

            // Initialize encryption engine
            EncryptionEngine engine = new EncryptionEngine();
            boolean initialized = engine.initializeEncryption(key);
            System.out.println("Encryption engine initialized: " + initialized);

            if (initialized) {
                String testData = "Hello, World! This is a secret message.";
                byte[] plaintext = testData.getBytes("UTF-8");

                // Encrypt the data
                byte[] encrypted = engine.encryptData(plaintext);
                System.out.println("Encrypted data length: " + encrypted.length);

                // Decrypt the data
                byte[] decrypted = engine.decryptData(encrypted);
                String result = new String(decrypted, "UTF-8");
                System.out.println("Decrypted message: " + result);

                // Cleanup
                engine.cleanup();
            }

        } catch (Exception e) {
            System.err.println("Encryption error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}