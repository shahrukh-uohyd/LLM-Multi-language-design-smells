package com.example.processing;

public class TextProcessingOperation {

    // Load the native C library containing the JNI implementations
    static {
        System.loadLibrary("text_processing_lib");
    }

    /**
     * Executes the text processing operation to produce a derived output value.
     * 
     * @param inputText The raw text-based input.
     * @return The derived final output string.
     */
    public String process(String inputText) {
        if (inputText == null || inputText.isEmpty()) {
            throw new IllegalArgumentException("Input text cannot be null or empty");
        }

        long stateHandle = 0;
        try {
            // 1. Parsing the input text into structured elements
            stateHandle = parseInputText(inputText);
            if (stateHandle == 0) {
                throw new RuntimeException("Native error: Failed to parse input text.");
            }

            // 2. Processing the structured elements according to predefined logic
            processElements(stateHandle);

            // 3. Generating a final output from the processed elements
            return generateOutput(stateHandle);

        } finally {
            // 4. Guaranteed cleanup of native C memory to prevent memory leaks
            if (stateHandle != 0) {
                freeNativeState(stateHandle);
            }
        }
    }

    // --- Native Method Declarations ---

    // Parses text, allocates a C struct, and returns its memory address as a handle
    private native long parseInputText(String input);

    // Processes the structured elements in the C struct referenced by the handle
    private native void processElements(long handle);

    // Extracts the generated final output from the C struct back into a Java String
    private native String generateOutput(long handle);

    // Frees the allocated C memory associated with the handle
    private native void freeNativeState(long handle);
}