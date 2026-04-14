// src/java/TextTransformer.java
public class TextTransformer {
    static {
        try {
            System.loadLibrary("texttransformer"); // Load native library
        } catch (UnsatisfiedLinkError e) {
            System.err.println("Native text transformer library failed to load: " + e.getMessage());
        }
    }
    
    // Native method declaration
    public native String[] transformToUppercase(String[] inputStrings);
    
    // Wrapper method for easier usage
    public String[] convertToUppercase(String[] inputStrings) {
        return transformToUppercase(inputStrings);
    }
    
    public static void main(String[] args) {
        TextTransformer transformer = new TextTransformer();
        
        // Create sample input array (simulating large array)
        String[] input = new String[1000];
        for (int i = 0; i < 1000; i++) {
            input[i] = "sample_text_" + i + "_with_mixed_case";
        }
        
        // Transform to uppercase
        long startTime = System.currentTimeMillis();
        String[] result = transformer.convertToUppercase(input);
        long endTime = System.currentTimeMillis();
        
        System.out.println("Processed " + input.length + " strings in " + (endTime - startTime) + " ms");
        System.out.println("First few results:");
        for (int i = 0; i < Math.min(5, result.length); i++) {
            System.out.println(input[i] + " -> " + result[i]);
        }
    }
}