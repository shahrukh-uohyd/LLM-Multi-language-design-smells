import java.util.List;

/**
 * Represents the processed elements after applying logic
 */
public class ProcessedElements {
    private List<String> processedElements;
    private List<String> processingLog;
    private String processingLogic;
    
    public ProcessedElements(List<String> processedElements, List<String> processingLog, String processingLogic) {
        this.processedElements = processedElements;
        this.processingLog = processingLog;
        this.processingLogic = processingLogic;
    }
    
    public List<String> getProcessedElements() {
        return processedElements;
    }
    
    public List<String> getProcessingLog() {
        return processingLog;
    }
    
    public String getProcessingLogic() {
        return processingLogic;
    }
}