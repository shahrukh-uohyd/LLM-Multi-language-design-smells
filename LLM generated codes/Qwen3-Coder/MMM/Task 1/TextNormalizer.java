package com.example;

/**
 * Text normalization utility using JNI for high-performance Unicode operations.
 * Performs: trimming, case folding, and whitespace normalization.
 */
public class TextNormalizer {
    
    // Load native library
    static {
        try {
            System.loadLibrary("text_normalizer");
        } catch (UnsatisfiedLinkError e) {
            // Fallback to relative path for development
            System.load("./lib/libtext_normalizer.so"); // Linux/macOS
            // For Windows: System.load("./lib/text_normalizer.dll");
        }
    }

    /**
     * Native method to normalize text.
     * Operations performed:
     * 1. Trim leading/trailing whitespace (Unicode-aware)
     * 2. Convert to lowercase (Unicode case folding)
     * 3. Normalize internal whitespace (collapse multiple spaces/tabs/newlines to single space)
     *
     * @param input Text to normalize (may be null)
     * @return Normalized text, or null if input was null
     */
    public native String normalize(String input);

    /**
     * Pure Java fallback implementation (used if JNI fails)
     */
    public String normalizeJavaFallback(String input) {
        if (input == null) return null;
        
        // Unicode-aware trimming using regex
        String trimmed = input.replaceAll("^\\s+", "").replaceAll("\\s+$", "");
        
        // Collapse internal whitespace sequences to single space
        String collapsed = trimmed.replaceAll("\\s+", " ");
        
        // Unicode case folding
        return collapsed.toLowerCase();
    }

    /**
     * Demonstration application
     */
    public static void main(String[] args) {
        TextNormalizer normalizer = new TextNormalizer();
        
        // Test cases covering various scenarios
        String[] testInputs = {
            "  HELLO WORLD  ",
            "  \t  Mixed\t\tWHITESPACE \n \r  ",
            "CAFÉ ÉLÉPHANT",
            "Привет Мир",  // Cyrillic
            "  𝓗𝓮𝓵𝓵𝓸 𝓦𝓸𝓻𝓵𝓭  ", // Mathematical script (Unicode)
            "Straße",      // German sharp S (ß -> ss in full case folding)
            null,
            "",
            "   "
        };

        System.out.println("=== JNI Text Normalization Demo ===\n");
        
        for (int i = 0; i < testInputs.length; i++) {
            String input = testInputs[i];
            String normalized;
            
            try {
                normalized = normalizer.normalize(input);
            } catch (Exception e) {
                System.err.println("JNI failed for input #" + i + ", using Java fallback");
                normalized = normalizer.normalizeJavaFallback(input);
            }
            
            System.out.printf("Input #%d : %s\n", i, 
                input == null ? "null" : ("\"" + input.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\""));
            System.out.printf("Normalized: %s\n\n", 
                normalized == null ? "null" : ("\"" + normalized + "\""));
        }
        
        System.out.println("=== Demo Complete ===");
    }
}