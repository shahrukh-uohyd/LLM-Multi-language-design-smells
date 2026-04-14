// File: src/main/java/com/example/image/ImageEncoder.java
package com.example.image;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;

/**
 * JNI-based image encoder that relies on native code for image encoding operations.
 * This class provides a secure way to load and use native libraries for image processing.
 */
public class ImageEncoder {
    static {
        // Load the native library securely
        System.loadLibrary("imageencoder");
    }

    /**
     * Encodes image data using native functionality
     * @param imageData raw image data to encode
     * @param format target encoding format (e.g., "JPEG", "PNG")
     * @return encoded image data
     */
    public native byte[] encodeImageData(byte[] imageData, String format);

    /**
     * Compresses image data using native compression algorithms
     * @param imageData raw image data
     * @return compressed image data
     */
    public native byte[] compressImage(byte[] imageData);

    /**
     * Validates if the native library was loaded successfully
     * @return true if native methods are available
     */
    public native boolean isNativeLibraryLoaded();

    /**
     * Gets version information about the native encoder
     * @return version string
     */
    public native String getNativeVersion();

    /**
     * Provides a Java fallback for image encoding in case native fails
     * This implements a basic compression algorithm as a fallback
     * @param imageData input image data
     * @return compressed data
     * @throws IOException if compression fails
     */
    public byte[] encodeWithFallback(byte[] imageData) throws IOException {
        // Fallback implementation using Java's built-in Deflater
        Deflater deflater = new Deflater();
        deflater.setInput(imageData);
        deflater.finish();
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (!deflater.finished()) {
            int count = deflater.deflate(buffer);
            outputStream.write(buffer, 0, count);
        }
        deflater.end();
        
        return outputStream.toByteArray();
    }

    /**
     * Safe wrapper for native encoding with error handling
     * @param imageData raw image data to encode
     * @param format target encoding format
     * @return encoded image data or null if operation fails
     */
    public byte[] safeEncodeImageData(byte[] imageData, String format) {
        try {
            if (!isNativeLibraryLoaded()) {
                System.err.println("Native library not loaded, using fallback");
                return encodeWithFallback(imageData);
            }
            
            // Validate inputs before passing to native code
            if (imageData == null || format == null || format.trim().isEmpty()) {
                throw new IllegalArgumentException("Invalid input parameters");
            }
            
            // Perform the native encoding operation
            byte[] result = encodeImageData(imageData, format);
            
            if (result == null) {
                System.err.println("Native encoding failed, using fallback");
                return encodeWithFallback(imageData);
            }
            
            return result;
        } catch (Exception e) {
            System.err.println("Error during native encoding: " + e.getMessage());
            try {
                return encodeWithFallback(imageData);
            } catch (IOException ioEx) {
                System.err.println("Both native and fallback encoding failed: " + ioEx.getMessage());
                return null;
            }
        }
    }
}