// Main.java - Example usage
public class Main {
    public static void main(String[] args) {
        DiagnosticLogger logger = new DiagnosticLogger();
        SecureCommunicationHandler secureComm = new SecureCommunicationHandler();
        
        // Test diagnostic functionality
        System.out.println("=== Testing Diagnostic Functionality ===");
        logger.logDiagnosticWithTimestamp("Application started");
        System.out.println(logger.getDetailedSystemDiagnostics());
        System.out.println("Memory usage: " + logger.getMemoryUsage() + " bytes");
        
        // Test encryption/decryption
        System.out.println("\n=== Testing Encryption/Decryption ===");
        String sensitiveData = "This is sensitive information that needs to be encrypted!";
        byte[] data = sensitiveData.getBytes();
        
        // Generate encryption key
        byte[] key = logger.generateKey(32); // 256-bit key
        System.out.println("Generated key length: " + key.length + " bytes");
        
        // Encrypt the data
        byte[] encryptedData = logger.encryptAES(data, key);
        System.out.println("Original data length: " + data.length);
        System.out.println("Encrypted data length: " + encryptedData.length);
        
        // Decrypt the data
        byte[] decryptedData = logger.decryptAES(encryptedData, key);
        String decryptedString = new String(decryptedData);
        System.out.println("Decryption successful: " + sensitiveData.equals(decryptedString));
        
        // Test hashing
        System.out.println("\n=== Testing Hashing ===");
        byte[] hash256 = logger.hashSHA256(data);
        byte[] hash512 = logger.hashSHA512(data);
        System.out.println("SHA-256 hash: " + DiagnosticLogger.bytesToHex(hash256));
        System.out.println("SHA-512 hash: " + DiagnosticLogger.bytesToHex(hash512));
        
        // Test secure communication
        System.out.println("\n=== Testing Secure Communication ===");
        String recipient = "server.example.com";
        String message = "Secure communication test message";
        
        // Generate session key
        byte[] sessionKey = secureComm.generateSessionKey("session_123", 32);
        if (sessionKey != null) {
            System.out.println("Session key generated successfully");
            
            // Send secure message
            boolean sent = secureComm.transmitSecureMessage(recipient, message, sessionKey);
            System.out.println("Message sent securely: " + sent);
            
            // Send another message to test sequence numbers
            secureComm.transmitSecureMessage(recipient, "Second message", sessionKey);
        }
        
        // Test authentication and integrity
        System.out.println("\n=== Testing Authentication and Integrity ===");
        byte[] privateKey = logger.generateKey(32); // Placeholder for private key
        byte[] publicKey = logger.generateKey(32);  // Placeholder for public key
        
        byte[] authenticatedEncrypted = secureComm.authenticateAndEncrypt(data, key, privateKey);
        if (authenticatedEncrypted != null) {
            System.out.println("Data authenticated and encrypted successfully");
            
            byte[] verifiedDecrypted = secureComm.decryptAndVerify(authenticatedEncrypted, key, publicKey);
            if (verifiedDecrypted != null) {
                System.out.println("Data verified and decrypted successfully");
                System.out.println("Content matches: " + sensitiveData.equals(new String(verifiedDecrypted)));
            }
        }
        
        // Test data integrity
        byte[] hashForVerification = secureComm.hashForVerification(data);
        boolean integrityValid = secureComm.verifyDataIntegrity(data, hashForVerification);
        System.out.println("Data integrity verification: " + integrityValid);
        
        // Final diagnostics
        System.out.println("\n=== Final Statistics ===");
        System.out.println("Active sessions: " + secureComm.getActiveSessionsCount());
        logger.logDiagnosticWithTimestamp("Application completed successfully");
        logger.flushLogs();
    }
}