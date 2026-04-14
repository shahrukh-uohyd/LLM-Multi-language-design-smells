/**
 * Main class that implements the text processing operation using native methods
 */
public class TextProcessor {
    static {
        System.loadLibrary("textprocessor");
    }
    
    // Native method declarations
    private native long parseInputTextNative(String inputText);
    private native long processStructuredElementsNative(long parsedElementsHandle, String processingLogic);
    private native String generateOutputFromProcessedElementsNative(long processedElementsHandle);
    private native void cleanupHandleNative(long handle);
    
    /**
     * Executes the complete text processing operation
     */
    public TextProcessingResult processText(TextInput input) {
        return processText(input, "default_processing_logic");
    }
    
    /**
     * Executes the complete text processing operation with custom processing logic
     */
    public TextProcessingResult processText(TextInput input, String processingLogic) {
        long startTime = System.nanoTime();
        long parsedElementsHandle = 0;
        long processedElementsHandle = 0;
        
        try {
            // Step 1: Parse the input text into structured elements
            parsedElementsHandle = parseInputTextNative(input.getText());
            if (parsedElementsHandle == 0) {
                return new TextProcessingResult(null, false, "Failed to parse input text", 
                    System.nanoTime() - startTime);
            }
            
            // Step 2: Process the structured elements according to predefined logic
            processedElementsHandle = processStructuredElementsNative(parsedElementsHandle, processingLogic);
            if (processedElementsHandle == 0) {
                return new TextProcessingResult(null, false, "Failed to process structured elements", 
                    System.nanoTime() - startTime);
            }
            
            // Step 3: Generate final output from the processed elements
            String finalOutput = generateOutputFromProcessedElementsNative(processedElementsHandle);
            if (finalOutput == null) {
                return new TextProcessingResult(null, false, "Failed to generate final output", 
                    System.nanoTime() - startTime);
            }
            
            long endTime = System.nanoTime();
            return new TextProcessingResult(finalOutput, true, null, endTime - startTime);
            
        } catch (Exception e) {
            long endTime = System.nanoTime();
            return new TextProcessingResult(null, false, 
                "Exception during processing: " + e.getMessage(), endTime - startTime);
        } finally {
            // Clean up native handles to prevent memory leaks
            if (parsedElementsHandle != 0) {
                cleanupHandleNative(parsedElementsHandle);
            }
            if (processedElementsHandle != 0) {
                cleanupHandleNative(processedElementsHandle);
            }
        }
    }
    
    /**
     * Convenience method to process text directly from string
     */
    public TextProcessingResult processText(String inputText) {
        return processText(new TextInput(inputText));
    }
    
    /**
     * Convenience method to process text with custom processing logic
     */
    public TextProcessingResult processText(String inputText, String processingLogic) {
        return processText(new TextInput(inputText), processingLogic);
    }
}