package com.example.processor;

public class DataProcessor {
    
    // Load the native library containing the JNI C++ implementations
    static {
        System.loadLibrary("dataprocessor_lib");
    }

    /**
     * The high-level operation requested by the system.
     * Processes structured input data and produces a final result.
     * 
     * @param inputData The raw structured string input.
     * @return The computed final result.
     */
    public String process(String inputData) {
        if (inputData == null || inputData.isEmpty()) {
            throw new IllegalArgumentException("Input data cannot be null or empty");
        }

        long dataHandle = 0;
        try {
            // 1. Parsing raw input into an internal C++ representation
            dataHandle = parseInput(inputData);
            if (dataHandle == 0) {
                throw new RuntimeException("Native error: Failed to parse input data.");
            }

            // 2. Normalizing the parsed data according to predefined rules
            normalizeData(dataHandle);

            // 3. Computing a final result based on the normalized data
            return computeResult(dataHandle);

        } finally {
            // 4. Cleanup: Ensure native memory is strictly freed to prevent memory leaks
            if (dataHandle != 0) {
                releaseData(dataHandle);
            }
        }
    }

    // --- Native Method Declarations ---
    
    // Parses input and returns a pointer (cast to long) to the C++ internal data structure
    private native long parseInput(String input);
    
    // Mutates the internal C++ data structure by applying normalization rules
    private native void normalizeData(long handle);
    
    // Computes and returns the final string result from the internal C++ structure
    private native String computeResult(long handle);
    
    // Frees the allocated C++ memory for the internal data structure
    private native void releaseData(long handle);
}