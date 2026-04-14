package com.example;

public class ClassMetadataRetriever {
    static {
        // Load the native library
        System.loadLibrary("native_metadata");
    }
    
    // Native method declaration
    public native String getClassMetadata(String className);
    
    public static void main(String[] args) {
        ClassMetadataRetriever retriever = new ClassMetadataRetriever();
        
        // Test with different classes
        String[] classNames = {
            "com.example.Person",
            "com.example.Student",
            "java.lang.String",
            "java.util.ArrayList"
        };
        
        for (String className : classNames) {
            try {
                String metadata = retriever.getClassMetadata(className);
                System.out.println("=== Metadata for " + className + " ===");
                System.out.println(metadata);
                System.out.println();
            } catch (Exception e) {
                System.err.println("Error retrieving metadata for " + className + ": " + e.getMessage());
            }
        }
    }
}