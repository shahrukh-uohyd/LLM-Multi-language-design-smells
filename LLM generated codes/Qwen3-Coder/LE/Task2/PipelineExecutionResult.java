/**
 * Result of pipeline execution
 */
public class PipelineExecutionResult {
    private byte[] output;
    private boolean success;
    private String errorMessage;
    private long executionTime;
    
    public PipelineExecutionResult(byte[] output, boolean success, String errorMessage, long executionTime) {
        this.output = output;
        this.success = success;
        this.errorMessage = errorMessage;
        this.executionTime = executionTime;
    }
    
    public byte[] getOutput() { return output; }
    public boolean isSuccess() { return success; }
    public String getErrorMessage() { return errorMessage; }
    public long getExecutionTime() { return executionTime; }
}