// File: src/main/java/com/example/image/ImageEncodingApp.java
package com.example.image;

import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import java.util.Base64;

/**
 * Main application demonstrating JNI-based image encoding
 */
public class ImageEncodingApp {
    private ImageEncoder encoder;

    public ImageEncodingApp() {
        this.encoder = new ImageEncoder();
    }

    /**
     * Demonstrates image encoding using native functionality
     * @param inputFile path to input image file
     * @param outputFile path to output encoded file
     * @param format encoding format
     */
    public void processImage(String inputFile, String outputFile, String format) {
        try {
            // Check if native library is loaded
            if (!encoder.isNativeLibraryLoaded()) {
                System.out.println("Warning: Native library is not loaded");
            } else {
                System.out.println("Native library version: " + encoder.getNativeVersion());
            }

            // Read image file
            BufferedImage originalImage = ImageIO.read(new File(inputFile));
            if (originalImage == null) {
                System.err.println("Could not read image from: " + inputFile);
                return;
            }

            // Convert image to byte array
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(originalImage, "png", baos);
            byte[] imageData = baos.toByteArray();

            System.out.println("Original image size: " + imageData.length + " bytes");

            // Encode using native functionality
            long startTime = System.currentTimeMillis();
            byte[] encodedData = encoder.safeEncodeImageData(imageData, format);
            long endTime = System.currentTimeMillis();

            if (encodedData != null) {
                System.out.println("Encoded image size: " + encodedData.length + " bytes");
                System.out.println("Processing time: " + (endTime - startTime) + " ms");
                
                // Write encoded image to file
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    fos.write(encodedData);
                }
                
                // Also write as base64 for verification
                String encodedBase64 = Base64.getEncoder().encodeToString(encodedData);
                System.out.println("Encoded image (first 100 chars): " + 
                                 encodedBase64.substring(0, Math.min(100, encodedBase64.length())) + "...");
                
                System.out.println("Encoded image saved to: " + outputFile);
            } else {
                System.err.println("Failed to encode image");
            }
        } catch (Exception e) {
            System.err.println("Error processing image: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Tests various encoding formats
     */
    public void testEncodingFormats() {
        System.out.println("\nTesting different encoding formats:");
        
        // Create a simple test image as byte array
        byte[] testData = new byte[1000];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = (byte) (i % 256);
        }
        
        String[] formats = {"JPEG", "PNG", "WEBP", "BMP"};
        
        for (String format : formats) {
            long startTime = System.currentTimeMillis();
            byte[] result = encoder.safeEncodeImageData(testData, format);
            long endTime = System.currentTimeMillis();
            
            if (result != null) {
                System.out.printf("%s format - Original: %d bytes, Encoded: %d bytes, Time: %d ms%n", 
                                format, testData.length, result.length, (endTime - startTime));
            } else {
                System.out.printf("%s format - Encoding failed%n", format);
            }
        }
    }

    /**
     * Creates a sample image for testing
     */
    public static void createSampleImage(String filename) {
        try {
            BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
            for (int x = 0; x < 200; x++) {
                for (int y = 0; y < 200; y++) {
                    int red = (x * 255) / 199;
                    int green = (y * 255) / 199;
                    int blue = ((x + y) * 255) / 398;
                    int rgb = (red << 16) | (green << 8) | blue;
                    img.setRGB(x, y, rgb);
                }
            }
            
            ImageIO.write(img, "png", new File(filename));
            System.out.println("Sample image created: " + filename);
        } catch (Exception e) {
            System.err.println("Error creating sample image: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("JNI-Based Image Encoding Module");
        System.out.println("==================================");
        
        // Create a sample image for testing
        String sampleImagePath = "sample_input.png";
        createSampleImage(sampleImagePath);
        
        ImageEncodingApp app = new ImageEncodingApp();
        
        // Test different encoding formats
        app.testEncodingFormats();
        
        // Process the sample image
        app.processImage(sampleImagePath, "encoded_output.jpg", "JPEG");
        
        System.out.println("\nApplication completed successfully.");
    }
}