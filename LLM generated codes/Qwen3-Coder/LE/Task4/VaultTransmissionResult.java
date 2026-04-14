/**
 * Result of vault transmission operation
 */
public class VaultTransmissionResult {
    private boolean success;
    private String transactionId;
    private String errorMessage;
    private long transmissionTime;
    
    public VaultTransmissionResult(boolean success, String transactionId, String errorMessage, long transmissionTime) {
        this.success = success;
        this.transactionId = transactionId;
        this.errorMessage = errorMessage;
        this.transmissionTime = transmissionTime;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public long getTransmissionTime() {
        return transmissionTime;
    }
}