package com.jni.metadata;

/**
 * Declares the JNI native method that retrieves class metadata.
 * The native library must be loaded before calling the native method.
 */
public class ClassMetadataJNI {

    // Load the native shared library
    static {
        System.loadLibrary("ClassMetadataJNI");
    }

    /**
     * Native method that locates the given class by name and
     * returns a formatted string with its metadata.
     *
     * @param className The fully-qualified class name (e.g., "com/jni/metadata/SampleClass")
     * @return A multi-line String with class metadata, or an error message.
     */
    public native String getClassMetadata(String className);
}