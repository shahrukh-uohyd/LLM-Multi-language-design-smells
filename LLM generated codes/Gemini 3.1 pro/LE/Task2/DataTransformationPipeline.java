package com.example.pipeline;

public class DataTransformationPipeline {

    // Load the native library containing the JNI implementations
    static {
        System.loadLibrary("transform_pipeline_lib");
    }

    /**
     * Executes the full data transformation pipeline.
     * 
     * @param rawData The low-level raw input data.
     * @return The fully transformed output data.
     */
    public byte[] executePipeline(byte[] rawData) {
        if (rawData == null || rawData.length == 0) {
            throw new IllegalArgumentException("Input data cannot be null or empty");
        }

        long pipelineHandle = 0;
        try {
            // 1. Read and interpret low-level data formats
            pipelineHandle = readAndInterpret(rawData);
            if (pipelineHandle == 0) {
                throw new RuntimeException("Native error: Failed to interpret raw data.");
            }

            // 2. Apply transformation rules to the interpreted data
            applyTransformationRules(pipelineHandle);

            // 3. Generate a transformed output
            return generateTransformedOutput(pipelineHandle);

        } finally {
            // 4. Guaranteed cleanup of native C++ memory
            if (pipelineHandle != 0) {
                cleanupPipeline(pipelineHandle);
            }
        }
    }

    // --- Native Method Declarations ---

    // Reads raw byte data, allocates a C++ context, and returns its memory address as a handle
    private native long readAndInterpret(byte[] rawData);

    // Applies the transformation rules to the C++ context referenced by the handle
    private native void applyTransformationRules(long handle);

    // Extracts the final transformed data from the C++ context back into a Java byte array
    private native byte[] generateTransformedOutput(long handle);

    // Frees the allocated C++ memory to prevent memory leaks
    private native void cleanupPipeline(long handle);
}