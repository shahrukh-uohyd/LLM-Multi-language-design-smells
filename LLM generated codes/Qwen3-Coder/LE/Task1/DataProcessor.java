// DataProcessor.java
public class DataProcessor {
    static {
        // Load the native library
        System.loadLibrary("dataprocessor"); // This loads libdataprocessor.so on Linux/Mac or dataprocessor.dll on Windows
    }
    
    // Native method declarations
    private native long parseInputNative(String rawData);
    private native long normalizeParsedDataNative(long parsedDataHandle);
    private native Object computeResultNative(long normalizedDataHandle);
    private native void cleanupHandleNative(long handle);
    
    /**
     * High-level operation that processes structured input data and produces a final result
     */
    public ProcessingResult processStructuredData(InputData inputData) {
        long parsedDataHandle = 0;
        long normalizedDataHandle = 0;
        
        try {
            // Step 1: Parse raw input into internal representation
            parsedDataHandle = parseInputNative(inputData.getRawData());
            if (parsedDataHandle == 0) {
                return new ProcessingResult(null, false, "Failed to parse input data");
            }
            
            // Step 2: Normalize the parsed data according to predefined rules
            normalizedDataHandle = normalizeParsedDataNative(parsedDataHandle);
            if (normalizedDataHandle == 0) {
                return new ProcessingResult(null, false, "Failed to normalize parsed data");
            }
            
            // Step 3: Compute final result based on normalized data
            Object finalResult = computeResultNative(normalizedDataHandle);
            if (finalResult == null) {
                return new ProcessingResult(null, false, "Failed to compute final result");
            }
            
            return new ProcessingResult(finalResult, true, null);
            
        } catch (Exception e) {
            return new ProcessingResult(null, false, "Exception during processing: " + e.getMessage());
        } finally {
            // Clean up native handles to prevent memory leaks
            if (parsedDataHandle != 0) {
                cleanupHandleNative(parsedDataHandle);
            }
            if (normalizedDataHandle != 0) {
                cleanupHandleNative(normalizedDataHandle);
            }
        }
    }
    
    /**
     * Convenience method to process input directly from string
     */
    public ProcessingResult processInput(String rawData) {
        InputData inputData = new InputData(rawData);
        return processStructuredData(inputData);
    }
}