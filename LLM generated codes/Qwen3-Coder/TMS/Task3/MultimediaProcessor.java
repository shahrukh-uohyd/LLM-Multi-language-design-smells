/**
 * Combined multimedia processor using both audio and video native features
 */
public class MultimediaProcessor {
    
    private final AudioMetadataExtractor audioExtractor;
    private final VideoFrameRotator videoRotator;
    
    public MultimediaProcessor() {
        this.audioExtractor = new AudioMetadataExtractor();
        this.videoRotator = new VideoFrameRotator();
    }
    
    /**
     * Process audio file to extract metadata
     * @param audioFilePath Path to audio file
     * @return Extracted audio metadata
     */
    public AudioMetadata processAudioMetadata(String audioFilePath) {
        return audioExtractor.extractMetadataFromFile(audioFilePath);
    }
    
    /**
     * Rotate video frame buffer
     * @param frameBuffer Original frame buffer
     * @param width Frame width in pixels
     * @param height Frame height in pixels
     * @param rotationAngle Rotation angle in degrees
     * @param pixelFormat Pixel format string
     * @return Rotated frame buffer
     */
    public byte[] processVideoRotation(byte[] frameBuffer, int width, int height, 
                                      int rotationAngle, String pixelFormat) {
        return videoRotator.rotateFrame(frameBuffer, width, height, rotationAngle, pixelFormat);
    }
    
    /**
     * Get specific audio metadata field
     * @param audioFilePath Path to audio file
     * @param field Metadata field name
     * @return Field value or null if not found
     */
    public String getAudioField(String audioFilePath, String field) {
        return audioExtractor.getSpecificMetadata(audioFilePath, field);
    }
    
    /**
     * Rotate video frame in-place for memory efficiency
     * @param frameBuffer Frame buffer to modify directly
     * @param width Frame width in pixels
     * @param height Frame height in pixels
     * @param rotationAngle Rotation angle in degrees
     * @return Success status
     */
    public boolean rotateFrameInPlace(byte[] frameBuffer, int width, int height, int rotationAngle) {
        return videoRotator.rotateFrameInPlace(frameBuffer, width, height, rotationAngle);
    }
}