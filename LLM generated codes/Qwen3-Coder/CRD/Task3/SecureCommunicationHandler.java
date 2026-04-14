// SecureCommunicationHandler.java
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class SecureCommunicationHandler {
    private DiagnosticLogger logger;
    private ConcurrentHashMap<String, byte[]> sessionKeys;
    private AtomicLong sequenceNumber;

    public SecureCommunicationHandler() {
        this.logger = new DiagnosticLogger();
        this.sessionKeys = new ConcurrentHashMap<>();
        this.sequenceNumber = new AtomicLong(0);
    }

    /**
     * Encrypts and sends data securely
     */
    public boolean sendSecureData(String recipient, byte[] data, byte[] key) {
        try {
            byte[] encryptedData = logger.encryptAES(data, key);
            
            // Log the secure transmission
            logger.logDiagnosticWithTimestamp("Secure data sent to: " + recipient);
            
            // Simulate sending data over network
            String encodedData = Base64.getEncoder().encodeToString(encryptedData);
            System.out.println("Sending encrypted data to " + recipient + ": " + encodedData.substring(0, Math.min(50, encodedData.length())) + "...");
            
            return true;
        } catch (Exception e) {
            logger.logDiagnosticWithTimestamp("Error in secure data transmission: " + e.getMessage());
            return false;
        }
    }

    /**
     * Receives and decrypts secure data
     */
    public byte[] receiveSecureData(String sender, byte[] encryptedData, byte[] key) {
        try {
            byte[] decryptedData = logger.decryptAES(encryptedData, key);
            
            // Log the secure reception
            logger.logDiagnosticWithTimestamp("Secure data received from: " + sender);
            
            return decryptedData;
        } catch (Exception e) {
            logger.logDiagnosticWithTimestamp("Error in secure data reception: " + e.getMessage());
            return null;
        }
    }

    /**
     * Authenticates and encrypts message with integrity check
     */
    public byte[] authenticateAndEncrypt(byte[] message, byte[] key, byte[] privateKey) {
        try {
            // First, sign the message for integrity
            byte[] signature = logger.signData(message, privateKey);
            
            // Then encrypt the message along with signature
            byte[] messageWithSignature = concatenateArrays(message, signature);
            return logger.encryptAES(messageWithSignature, key);
        } catch (Exception e) {
            logger.logDiagnosticWithTimestamp("Authentication and encryption failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Decrypts and verifies authenticated message
     */
    public byte[] decryptAndVerify(byte[] encryptedData, byte[] key, byte[] publicKey) {
        try {
            // First, decrypt the data
            byte[] decryptedData = logger.decryptAES(encryptedData, key);
            
            // Extract original message and signature (assuming signature is appended)
            int messageLength = decryptedData.length - 256; // Assuming RSA signature is 256 bytes
            if (messageLength <= 0) {
                throw new IllegalArgumentException("Invalid encrypted data format");
            }
            
            byte[] originalMessage = new byte[messageLength];
            byte[] signature = new byte[256];
            System.arraycopy(decryptedData, 0, originalMessage, 0, messageLength);
            System.arraycopy(decryptedData, messageLength, signature, 0, 256);
            
            // Verify the signature
            boolean isValid = logger.verifySignature(originalMessage, signature, publicKey);
            if (!isValid) {
                logger.logDiagnosticWithTimestamp("Signature verification failed!");
                return null;
            }
            
            logger.logDiagnosticWithTimestamp("Message verified successfully");
            return originalMessage;
        } catch (Exception e) {
            logger.logDiagnosticWithTimestamp("Decryption and verification failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Generates a secure session key
     */
    public byte[] generateSessionKey(String sessionId, int keyLength) {
        try {
            byte[] key = logger.generateKey(keyLength);
            sessionKeys.put(sessionId, key);
            
            logger.logDiagnosticWithTimestamp("Session key generated for: " + sessionId);
            return key;
        } catch (Exception e) {
            logger.logDiagnosticWithTimestamp("Session key generation failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets a stored session key
     */
    public byte[] getSessionKey(String sessionId) {
        return sessionKeys.get(sessionId);
    }

    /**
     * Hashes sensitive data before transmission
     */
    public byte[] hashForVerification(byte[] data) {
        try {
            byte[] hash = logger.hashSHA256(data);
            logger.logDiagnosticWithTimestamp("Data hashed for verification");
            return hash;
        } catch (Exception e) {
            logger.logDiagnosticWithTimestamp("Hashing failed: " + e.getMessage());
            return null;
        }
    }

    /**
     * Verifies data integrity using hash
     */
    public boolean verifyDataIntegrity(byte[] data, byte[] expectedHash) {
        try {
            byte[] calculatedHash = logger.hashSHA256(data);
            boolean isValid = logger.verifyHash(data, expectedHash, "SHA-256");
            
            logger.logDiagnosticWithTimestamp("Data integrity check: " + (isValid ? "PASSED" : "FAILED"));
            return isValid;
        } catch (Exception e) {
            logger.logDiagnosticWithTimestamp("Integrity verification failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Securely transmits data with sequence number
     */
    public boolean transmitSecureMessage(String recipient, String message, byte[] key) {
        try {
            long seqNum = sequenceNumber.incrementAndGet();
            String messageWithSeq = seqNum + ":" + message;
            
            byte[] encryptedMessage = logger.encryptAES(messageWithSeq.getBytes(), key);
            String encodedMessage = Base64.getEncoder().encodeToString(encryptedMessage);
            
            // Log the transmission
            logger.logDiagnosticWithTimestamp("Secure message transmitted to " + recipient + ", seq: " + seqNum);
            
            // Simulate network transmission
            System.out.println("Transmitting secure message to " + recipient + ": " + encodedMessage.substring(0, Math.min(50, encodedMessage.length())) + "...");
            
            return true;
        } catch (Exception e) {
            logger.logDiagnosticWithTimestamp("Secure transmission failed: " + e.getMessage());
            return false;
        }
    }

    private byte[] concatenateArrays(byte[] first, byte[] second) {
        byte[] result = new byte[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public int getActiveSessionsCount() {
        return sessionKeys.size();
    }
}