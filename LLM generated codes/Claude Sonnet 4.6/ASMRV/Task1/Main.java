package com.jni.metadata;

/**
 * Entry point for the JNI Class Metadata application.
 * Calls the native method and uses the returned metadata string.
 */
public class Main {

    public static void main(String[] args) {
        ClassMetadataJNI metadataFetcher = new ClassMetadataJNI();

        // Use JNI-style binary class name (slashes, not dots)
        String className = "com/jni/metadata/SampleClass";

        System.out.println("=== JNI Class Metadata Retrieval ===");
        System.out.println("Requesting metadata for: " + className.replace('/', '.'));
        System.out.println();

        // Call the native method — returns metadata as a String
        String metadata = metadataFetcher.getClassMetadata(className);

        // Use the returned value
        System.out.println("Retrieved Metadata:");
        System.out.println("-------------------");
        System.out.println(metadata);
    }
}