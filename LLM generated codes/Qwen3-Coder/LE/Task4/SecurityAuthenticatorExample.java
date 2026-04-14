import java.util.Arrays;

/**
 * Example usage of the security authentication sweep
 */
public class SecurityAuthenticatorExample {
    public static void main(String[] args) {
        SecurityAuthenticator authenticator = new SecurityAuthenticator();
        
        // Create sample biometric data
        byte[] biometricSample = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08,
                                  0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F, 0x10};
        BiometricData biometricData = new BiometricData(biometricSample, "fingerprint");
        
        // Perform authentication sweep
        String userId = "user123";
        String sessionId = "session_abc123";
        
        AuthenticationResult result = authenticator.performAuthenticationSweep(biometricData, userId, sessionId);
        
        if (result.isAuthenticated()) {
            System.out.println("Authentication SUCCESS for user: " + result.getUserId());
            System.out.println("Session ID: " + result.getSessionId());
            System.out.println("Total processing time: " + result.getTotalProcessingTime() + " nanoseconds");
            System.out.println("Transaction ID: " + result.getTransmissionResult().getTransactionId());
        } else {
            System.out.println("Authentication FAILED for user: " + userId);
            System.out.println("Error: " + result.getErrorMessage());
        }
        
        // Test with different user
        String anotherUser = "admin456";
        byte[] anotherBiometricSample = {0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, 0x18,
                                         0x19, 0x1A, 0x1B, 0x1C, 0x1D, 0x1E, 0x1F, 0x20};
        BiometricData anotherBiometricData = new BiometricData(anotherBiometricSample, "iris_scan");
        
        AuthenticationResult anotherResult = authenticator.authenticate(anotherBiometricData, anotherUser);
        
        if (anotherResult.isAuthenticated()) {
            System.out.println("\nAuthentication SUCCESS for user: " + anotherResult.getUserId());
            System.out.println("Session ID: " + anotherResult.getSessionId());
            System.out.println("Total processing time: " + anotherResult.getTotalProcessingTime() + " nanoseconds");
        } else {
            System.out.println("\nAuthentication FAILED for user: " + anotherUser);
            System.out.println("Error: " + anotherResult.getErrorMessage());
        }
    }
}