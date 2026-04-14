/**
 * Main class implementing the authentication sweep using native C++ library
 */
public class SecurityAuthenticator {
    static {
        System.loadLibrary("securityauthenticator");
    }
    
    // Native method declarations
    private native long extractBiometricMinutiaeNative(byte[] rawData);
    private native long generateCryptographicSignatureNative(long featuresHandle, String userId);
    private native boolean transmitToVaultNative(long signatureHandle, String userId, String sessionId);
    private native void cleanupHandleNative(long handle);
    
    /**
     * Performs a complete authentication sweep with existing native capabilities
     */
    public AuthenticationResult performAuthenticationSweep(BiometricData biometricData, String userId, String sessionId) {
        long startTime = System.nanoTime();
        long featuresHandle = 0;
        long signatureHandle = 0;
        
        try {
            // Step 1: Extracting biometric minutiae from a raw data buffer
            featuresHandle = extractBiometricMinutiaeNative(biometricData.getRawData());
            if (featuresHandle == 0) {
                return new AuthenticationResult(false, userId, sessionId, 
                    System.nanoTime() - startTime, "Failed to extract biometric minutiae", null, null, null);
            }
            
            // Step 2: Generating a cryptographic signature for the extracted features
            signatureHandle = generateCryptographicSignatureNative(featuresHandle, userId);
            if (signatureHandle == 0) {
                return new AuthenticationResult(false, userId, sessionId, 
                    System.nanoTime() - startTime, "Failed to generate cryptographic signature", null, null, null);
            }
            
            // Step 3: Transmitting that signature to a secure hardware vault
            boolean transmissionSuccess = transmitToVaultNative(signatureHandle, userId, sessionId);
            VaultTransmissionResult transmissionResult = new VaultTransmissionResult(
                transmissionSuccess, 
                transmissionSuccess ? generateTransactionId(userId, sessionId) : null,
                transmissionSuccess ? null : "Failed to transmit to vault",
                System.nanoTime() - startTime
            );
            
            if (!transmissionSuccess) {
                return new AuthenticationResult(false, userId, sessionId, 
                    System.nanoTime() - startTime, "Failed to transmit to vault", null, null, transmissionResult);
            }
            
            // Success case
            long endTime = System.nanoTime();
            return new AuthenticationResult(true, userId, sessionId, 
                endTime - startTime, null, null, null, transmissionResult);
            
        } catch (Exception e) {
            long endTime = System.nanoTime();
            return new AuthenticationResult(false, userId, sessionId, 
                endTime - startTime, "Exception during authentication: " + e.getMessage(), null, null, null);
        } finally {
            // Clean up native handles to prevent memory leaks
            if (featuresHandle != 0) {
                cleanupHandleNative(featuresHandle);
            }
            if (signatureHandle != 0) {
                cleanupHandleNative(signatureHandle);
            }
        }
    }
    
    /**
     * Convenience method for authentication without explicit session ID
     */
    public AuthenticationResult authenticate(BiometricData biometricData, String userId) {
        String sessionId = generateSessionId(userId);
        return performAuthenticationSweep(biometricData, userId, sessionId);
    }
    
    private String generateSessionId(String userId) {
        return "sess_" + userId + "_" + System.currentTimeMillis();
    }
    
    private String generateTransactionId(String userId, String sessionId) {
        return "txn_" + userId + "_" + sessionId + "_" + System.currentTimeMillis();
    }
}