// TransactionResult.java
public class TransactionResult {
    private String transactionId;
    private boolean success;
    private String errorMessage;
    private long nativeCommitTimeMs;
    
    public TransactionResult(String transactionId, boolean success, String errorMessage, long commitTime) {
        this.transactionId = transactionId;
        this.success = success;
        this.errorMessage = errorMessage;
        this.nativeCommitTimeMs = commitTime;
    }
    
    public String getTransactionId() { return transactionId; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    public long getNativeCommitTimeMs() { return nativeCommitTimeMs; }
    
    @Override
    public String toString() {
        return String.format("TransactionResult{id='%s', success=%s, error='%s', time=%dms}", 
                           transactionId, success, errorMessage, nativeCommitTimeMs);
    }
}