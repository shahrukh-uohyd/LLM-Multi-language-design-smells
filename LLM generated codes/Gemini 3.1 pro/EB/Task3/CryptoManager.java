public class CryptoManager {
    static {
        System.loadLibrary("crypto_module");
    }

    // The native method defined in C++ takes the SecurityObject as a parameter
    public native void initializeEncryptionEngine(SecurityObject securityObj);

    public static void main(String[] args) {
        // Generate a mock 256-bit (32-byte) AES Key
        byte[] myKey = new byte[32]; 
        
        SecurityObject secObj = new SecurityObject(myKey);
        CryptoManager manager = new CryptoManager();
        
        // Pass the object to C++
        manager.initializeEncryptionEngine(secObj);
    }
}