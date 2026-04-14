public class DataPrivacyModule {

    static {
        System.loadLibrary("wearable_health_native");
    }

    // Native declarations for cryptography
    private native byte[] generateSecureKey();
    private native byte[] encryptHealthData(byte[] rawData, byte[] key);

    /**
     * Secures plaintext health metrics using a dynamically generated native key.
     * 
     * @param healthMetrics Plaintext string (e.g., JSON) of health data.
     * @return The encrypted payload ready for secure transmission.
     */
    public byte[] secureData(String healthMetrics) {
        System.out.println("Initiating hardware-backed data encryption...");
        
        // 1. Generate a secure, one-time session key via native crypto API
        byte[] sessionKey = generateSecureKey();
        
        if (sessionKey == null) {
            throw new SecurityException("Fatal Error: Failed to generate secure cryptographic key.");
        }
        
        // 2. Encrypt the raw data payload
        byte[] rawBytes = healthMetrics.getBytes();
        byte[] encryptedPayload = encryptHealthData(rawBytes, sessionKey);
        
        System.out.println("Data successfully encrypted. Payload size: " + encryptedPayload.length + " bytes.");
        
        return encryptedPayload;
    }
}