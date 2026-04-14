/**
 * Complete result of the authentication sweep
 */
public class AuthenticationResult {
    private boolean authenticated;
    private String userId;
    private String sessionId;
    private long totalProcessingTime;
    private String errorMessage;
    private BiometricFeatures extractedFeatures;
    private CryptographicSignature generatedSignature;
    private VaultTransmissionResult transmissionResult;
    
    public AuthenticationResult(boolean authenticated, String userId, String sessionId, 
                              long totalProcessingTime, String errorMessage,
                              BiometricFeatures extractedFeatures,
                              CryptographicSignature generatedSignature,
                              VaultTransmissionResult transmissionResult) {
        this.authenticated = authenticated;
        this.userId = userId;
        this.sessionId = sessionId;
        this.totalProcessingTime = totalProcessingTime;
        this.errorMessage = errorMessage;
        this.extractedFeatures = extractedFeatures;
        this.generatedSignature = generatedSignature;
        this.transmissionResult = transmissionResult;
    }
    
    public boolean isAuthenticated() {
        return authenticated;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public long getTotalProcessingTime() {
        return totalProcessingTime;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public BiometricFeatures getExtractedFeatures() {
        return extractedFeatures;
    }
    
    public CryptographicSignature getGeneratedSignature() {
        return generatedSignature;
    }
    
    public VaultTransmissionResult getTransmissionResult() {
        return transmissionResult;
    }
}