import java.util.List;

/**
 * Main class that implements the data transformation pipeline
 */
public class TransformationPipeline {
    static {
        System.loadLibrary("transformationpipeline");
    }
    
    // Native method declarations
    private native long interpretLowLevelDataNative(byte[] lowLevelData);
    private native long applyTransformationRulesNative(long interpretedDataHandle, TransformationRule[] rules);
    private native byte[] generateTransformedOutputNative(long transformedDataHandle);
    private native void cleanupHandleNative(long handle);
    
    /**
     * Execute the full transformation pipeline
     */
    public byte[] executePipeline(byte[] lowLevelData, List<TransformationRule> rules) {
        long interpretedDataHandle = 0;
        long transformedDataHandle = 0;
        
        try {
            // Step 1: Read and interpret low-level data formats
            interpretedDataHandle = interpretLowLevelDataNative(lowLevelData);
            if (interpretedDataHandle == 0) {
                throw new RuntimeException("Failed to interpret low-level data");
            }
            
            // Convert List to array for JNI
            TransformationRule[] rulesArray = rules.toArray(new TransformationRule[0]);
            
            // Step 2: Apply transformation rules to the interpreted data
            transformedDataHandle = applyTransformationRulesNative(interpretedDataHandle, rulesArray);
            if (transformedDataHandle == 0) {
                throw new RuntimeException("Failed to apply transformation rules");
            }
            
            // Step 3: Generate transformed output
            byte[] result = generateTransformedOutputNative(transformedDataHandle);
            if (result == null) {
                throw new RuntimeException("Failed to generate transformed output");
            }
            
            return result;
            
        } catch (Exception e) {
            throw new RuntimeException("Pipeline execution failed: " + e.getMessage(), e);
        } finally {
            // Clean up native handles
            if (interpretedDataHandle != 0) {
                cleanupHandleNative(interpretedDataHandle);
            }
            if (transformedDataHandle != 0) {
                cleanupHandleNative(transformedDataHandle);
            }
        }
    }
    
    /**
     * Convenience method to execute pipeline with default rules
     */
    public byte[] executePipeline(byte[] lowLevelData) {
        return executePipeline(lowLevelData, java.util.Collections.emptyList());
    }
}