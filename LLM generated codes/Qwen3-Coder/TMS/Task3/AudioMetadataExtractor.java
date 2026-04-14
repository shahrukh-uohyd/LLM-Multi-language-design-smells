/**
 * Native audio metadata extractor class
 */
public class AudioMetadataExtractor {
    
    static {
        System.loadLibrary("audio_metadata_native");
    }
    
    // Native method to extract metadata from audio file
    private native AudioMetadata nativeExtractMetadataFromFile(String filePath);
    
    // Native method to extract metadata from audio stream data
    private native AudioMetadata nativeExtractMetadataFromStream(byte[] audioData);
    
    // Native method to extract specific metadata fields
    private native String nativeGetSpecificMetadata(String filePath, String field);
    
    /**
     * Extract comprehensive metadata from an audio file
     * @param filePath Path to the audio file
     * @return AudioMetadata object containing extracted information
     * @throws IllegalArgumentException if file path is invalid
     */
    public AudioMetadata extractMetadataFromFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("Audio file path cannot be null or empty");
        }
        AudioMetadata metadata = nativeExtractMetadataFromFile(filePath);
        if (metadata == null) {
            throw new RuntimeException("Failed to extract metadata from file: " + filePath);
        }
        return metadata;
    }
    
    /**
     * Extract metadata from audio stream data in byte array format
     * @param audioData Byte array containing audio stream data
     * @return AudioMetadata object containing extracted information
     * @throws IllegalArgumentException if audio data is null
     */
    public AudioMetadata extractMetadataFromStream(byte[] audioData) {
        if (audioData == null || audioData.length == 0) {
            throw new IllegalArgumentException("Audio data cannot be null or empty");
        }
        AudioMetadata metadata = nativeExtractMetadataFromStream(audioData);
        if (metadata == null) {
            throw new RuntimeException("Failed to extract metadata from audio stream");
        }
        return metadata;
    }
    
    /**
     * Get specific metadata field from audio file
     * @param filePath Path to the audio file
     * @param field Name of the metadata field to extract (e.g., "title", "artist", "duration")
     * @return Value of the specified metadata field, or null if not found
     */
    public String getSpecificMetadata(String filePath, String field) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("Audio file path cannot be null or empty");
        }
        if (field == null || field.isEmpty()) {
            throw new IllegalArgumentException("Metadata field cannot be null or empty");
        }
        return nativeGetSpecificMetadata(filePath, field);
    }
}