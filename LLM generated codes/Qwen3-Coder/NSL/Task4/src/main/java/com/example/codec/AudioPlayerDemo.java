// File: src/main/java/com/example/codec/AudioPlayerDemo.java
package com.example.codec;

import java.util.Arrays;
import java.util.Map;

/**
 * Demo application showing how to use the AudioCodecService
 */
public class AudioPlayerDemo {
    private AudioCodecService codecService;

    public AudioPlayerDemo() {
        this.codecService = new AudioCodecService();
    }

    /**
     * Demonstrates basic usage of the audio codec service
     */
    public void demonstrateUsage() {
        System.out.println("=== Audio Codec Service Demo ===\n");
        
        // Check if the service is ready
        if (!AudioCodecService.isReady()) {
            System.out.println("Audio codec service is not ready - native library not loaded");
            return;
        }
        
        System.out.println("Codec Version: " + codecService.getSafeCodecVersion());
        
        // Show supported formats
        String[] supportedFormats = codecService.getSafeSupportedFormats();
        System.out.println("Supported formats: " + Arrays.toString(supportedFormats));
        
        // Initialize with defaults
        boolean initSuccess = codecService.initializeWithDefaults();
        System.out.println("Initialization successful: " + initSuccess);
        
        // Example: Check if a specific format is supported
        String testFormat = "AAC";
        boolean isSupported = codecService.isFormatSupportedSafe(testFormat);
        System.out.println(testFormat + " format supported: " + isSupported);
        
        // Example: Simulate decoding some data
        String sampleData = "This is sample audio data for demonstration";
        byte[] decodedData = codecService.safeDecodeAudioData(sampleData.getBytes());
        System.out.println("Decoded data length: " + 
                          (decodedData != null ? decodedData.length : "null"));
        
        // Example: Simulate getting metadata (would require an actual file in real usage)
        System.out.println("Metadata example: " + 
                          codecService.safeGetAudioMetadata("sample_file.mp3"));
        
        // Cleanup resources
        codecService.safeCleanup();
        System.out.println("\nDemo completed successfully.");
    }

    public static void main(String[] args) {
        System.out.println("Audio Codec Service Integration Demo");
        System.out.println("====================================\n");
        
        AudioPlayerDemo demo = new AudioPlayerDemo();
        demo.demonstrateUsage();
    }
}