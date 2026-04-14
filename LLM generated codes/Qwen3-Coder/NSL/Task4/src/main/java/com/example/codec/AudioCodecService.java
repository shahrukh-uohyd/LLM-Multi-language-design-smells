// File: src/main/java/com/example/codec/AudioCodecService.java
package com.example.codec;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * JNI-based audio codec service that wraps the native 'audio-codec-v2' library.
 * This class provides a complete interface for decoding proprietary audio formats.
 */
public class AudioCodecService {
    private static final Logger logger = Logger.getLogger(AudioCodecService.class.getName());
    
    // Static block to load the native library
    static {
        try {
            // Attempt to load the native library
            loadNativeLibrary();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load native audio codec library: " + e.getMessage(), e);
            // Set up fallback behavior if needed
            nativeLibraryLoaded = false;
        }
    }
    
    // Flag to track if native library is loaded
    private static volatile boolean nativeLibraryLoaded = true;

    /**
     * Loads the native audio codec library with proper error handling
     */
    private static void loadNativeLibrary() throws Exception {
        try {
            // Load the native library by name
            System.loadLibrary("audio-codec-v2");
            logger.info("Successfully loaded native audio codec library 'audio-codec-v2'");
        } catch (UnsatisfiedLinkError e) {
            logger.log(Level.WARNING, "Native library 'audio-codec-v2' not found in system path", e);
            
            // Try to load from resources if available
            loadLibraryFromResources();
        }
    }

    /**
     * Attempts to load the native library from resources
     */
    private static void loadLibraryFromResources() throws IOException {
        String osName = System.getProperty("os.name").toLowerCase();
        String osArch = System.getProperty("os.arch").toLowerCase();
        
        String libraryExtension;
        String libraryPrefix = "lib";
        
        if (osName.contains("win")) {
            libraryExtension = ".dll";
            libraryPrefix = "";
        } else if (osName.contains("mac")) {
            libraryExtension = ".dylib";
        } else {
            libraryExtension = ".so";
        }
        
        String libraryFileName = libraryPrefix + "audio-codec-v2" + libraryExtension;
        
        // Try to extract and load from resources
        File tempFile = File.createTempFile("audio-codec-v2", libraryExtension);
        tempFile.deleteOnExit();
        
        try (InputStream is = AudioCodecService.class.getResourceAsStream("/lib/" + libraryFileName)) {
            if (is != null) {
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }
                
                System.load(tempFile.getAbsolutePath());
                logger.info("Successfully loaded native audio codec library from resources: " + tempFile.getAbsolutePath());
            } else {
                logger.severe("Native library not found in resources: /lib/" + libraryFileName);
                nativeLibraryLoaded = false;
                throw new UnsatisfiedLinkError("Native library not found in resources");
            }
        }
    }

    /**
     * Decodes an audio file using the native codec
     * @param filePath Path to the proprietary audio file
     * @return Decoded audio data as byte array
     */
    public native byte[] decodeAudioFile(String filePath);

    /**
     * Decodes audio data from a byte array
     * @param audioData Raw proprietary audio data
     * @return Decoded audio data
     */
    public native byte[] decodeAudioData(byte[] audioData);

    /**
     * Decodes audio data with specific configuration
     * @param audioData Raw proprietary audio data
     * @param config Configuration parameters for decoding
     * @return Decoded audio data
     */
    public native byte[] decodeAudioDataWithConfig(byte[] audioData, String config);

    /**
     * Gets metadata from a proprietary audio file
     * @param filePath Path to the audio file
     * @return Metadata as a map
     */
    public native Map<String, Object> getAudioMetadata(String filePath);

    /**
     * Gets supported audio formats
     * @return Array of supported format strings
     */
    public native String[] getSupportedFormats();

    /**
     * Checks if a specific format is supported
     * @param format Format to check
     * @return True if format is supported
     */
    public native boolean isFormatSupported(String format);

    /**
     * Gets the version of the native codec library
     * @return Version string
     */
    public native String getCodecVersion();

    /**
     * Initializes the codec with specific parameters
     * @param params Initialization parameters
     * @return True if initialization was successful
     */
    public native boolean initializeCodec(Map<String, String> params);

    /**
     * Cleans up resources used by the codec
     */
    public native void cleanupCodec();

    /**
     * Checks if the native library is loaded and functional
     * @return True if native library is available
     */
    public boolean isNativeLibraryAvailable() {
        return nativeLibraryLoaded;
    }

    /**
     * Safely decodes an audio file with error handling
     * @param filePath Path to the proprietary audio file
     * @return Decoded audio data or null if operation fails
     */
    public byte[] safeDecodeAudioFile(String filePath) {
        try {
            if (!nativeLibraryLoaded) {
                logger.warning("Native library not loaded, returning null for decode operation");
                return null;
            }
            
            // Validate input
            if (filePath == null || filePath.trim().isEmpty()) {
                throw new IllegalArgumentException("File path cannot be null or empty");
            }
            
            File audioFile = new File(filePath);
            if (!audioFile.exists()) {
                throw new FileNotFoundException("Audio file does not exist: " + filePath);
            }
            
            // Perform the native decoding operation
            return decodeAudioFile(filePath);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error decoding audio file: " + filePath, e);
            return null;
        }
    }

    /**
     * Safely decodes audio data with error handling
     * @param audioData Raw proprietary audio data
     * @return Decoded audio data or null if operation fails
     */
    public byte[] safeDecodeAudioData(byte[] audioData) {
        try {
            if (!nativeLibraryLoaded) {
                logger.warning("Native library not loaded, returning null for decode operation");
                return null;
            }
            
            // Validate input
            if (audioData == null) {
                throw new IllegalArgumentException("Audio data cannot be null");
            }
            
            // Perform the native decoding operation
            return decodeAudioData(audioData);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error decoding audio data", e);
            return null;
        }
    }

    /**
     * Gets audio metadata with error handling
     * @param filePath Path to the audio file
     * @return Metadata map or empty map if operation fails
     */
    public Map<String, Object> safeGetAudioMetadata(String filePath) {
        try {
            if (!nativeLibraryLoaded) {
                logger.warning("Native library not loaded, returning empty metadata map");
                return new ConcurrentHashMap<>();
            }
            
            // Validate input
            if (filePath == null || filePath.trim().isEmpty()) {
                throw new IllegalArgumentException("File path cannot be null or empty");
            }
            
            File audioFile = new File(filePath);
            if (!audioFile.exists()) {
                throw new FileNotFoundException("Audio file does not exist: " + filePath);
            }
            
            // Get metadata from native library
            Map<String, Object> metadata = getAudioMetadata(filePath);
            return metadata != null ? metadata : new ConcurrentHashMap<>();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error getting audio meta " + filePath, e);
            return new ConcurrentHashMap<>();
        }
    }

    /**
     * Initializes the codec with default parameters
     * @return True if initialization was successful
     */
    public boolean initializeWithDefaults() {
        if (!nativeLibraryLoaded) {
            logger.warning("Native library not loaded, cannot initialize codec");
            return false;
        }
        
        Map<String, String> defaultParams = new ConcurrentHashMap<>();
        defaultParams.put("buffer_size", "8192");
        defaultParams.put("quality", "high");
        defaultParams.put("threads", "2");
        
        return initializeCodec(defaultParams);
    }

    /**
     * Gets the codec version with error handling
     * @return Version string or "unknown" if operation fails
     */
    public String getSafeCodecVersion() {
        try {
            if (!nativeLibraryLoaded) {
                return "unknown (native library not loaded)";
            }
            
            String version = getCodecVersion();
            return version != null ? version : "unknown";
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting codec version", e);
            return "unknown";
        }
    }

    /**
     * Gets supported formats with error handling
     * @return Array of supported formats or empty array if operation fails
     */
    public String[] getSafeSupportedFormats() {
        try {
            if (!nativeLibraryLoaded) {
                logger.warning("Native library not loaded, returning empty format array");
                return new String[0];
            }
            
            String[] formats = getSupportedFormats();
            return formats != null ? formats : new String[0];
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error getting supported formats", e);
            return new String[0];
        }
    }

    /**
     * Checks if format is supported with error handling
     * @param format Format to check
     * @return True if supported, false otherwise
     */
    public boolean isFormatSupportedSafe(String format) {
        try {
            if (!nativeLibraryLoaded) {
                return false;
            }
            
            if (format == null) {
                return false;
            }
            
            return isFormatSupported(format);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error checking format support: " + format, e);
            return false;
        }
    }

    /**
     * Cleans up resources, calling native cleanup and handling errors
     */
    public void safeCleanup() {
        try {
            if (nativeLibraryLoaded) {
                cleanupCodec();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error during codec cleanup", e);
        }
    }

    /**
     * Convenience method to decode audio file and return as ByteBuffer
     * @param filePath Path to the audio file
     * @return ByteBuffer containing decoded audio data
     */
    public ByteBuffer decodeToByteBuffer(String filePath) {
        byte[] decodedData = safeDecodeAudioFile(filePath);
        if (decodedData != null) {
            return ByteBuffer.wrap(decodedData);
        }
        return null;
    }

    /**
     * Static method to check if the service is ready to use
     * @return True if native library is loaded and service is available
     */
    public static boolean isReady() {
        return nativeLibraryLoaded;
    }
}