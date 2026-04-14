/**
 * Represents the final result of the text processing operation
 */
public class TextProcessingResult {
    private String output;
    private boolean success;
    private String errorMessage;
    private long processingTime;
    
    public TextProcessingResult(String output, boolean success, String errorMessage, long processingTime) {
        this.output = output;
        this.success = success;
        this.errorMessage = errorMessage;
        this.processingTime = processingTime;
    }
    
    public String getOutput() {
        return output;
    }
    
    public boolean isSuccess() {
        return success;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public long getProcessingTime() {
        return processingTime;
    }
}