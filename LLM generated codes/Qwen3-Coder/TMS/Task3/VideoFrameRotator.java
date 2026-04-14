/**
 * Native video frame rotation utility
 */
public class VideoFrameRotator {
    
    static {
        System.loadLibrary("video_rotation_native");
    }
    
    // Native method to rotate video frame buffer
    private native byte[] nativeRotateFrame(byte[] frameBuffer, int width, int height, 
                                          int rotationAngle, String pixelFormat);
    
    // Native method to rotate frame in-place (modifies original buffer)
    private native boolean nativeRotateFrameInPlace(byte[] frameBuffer, int width, int height, 
                                                  int rotationAngle);
    
    // Native method to get required buffer size for rotated frame
    private native int nativeGetRotatedBufferSize(int width, int height, String pixelFormat);
    
    /**
     * Rotate a video frame buffer by the specified angle
     * @param frameBuffer Original frame buffer data
     * @param width Width of the video frame in pixels
     * @param height Height of the video frame in pixels
     * @param rotationAngle Rotation angle in degrees (90, 180, 270, etc.)
     * @param pixelFormat Pixel format string (e.g., "RGB24", "YUV420P", "NV12")
     * @return New byte array containing the rotated frame data
     * @throws IllegalArgumentException if parameters are invalid
     */
    public byte[] rotateFrame(byte[] frameBuffer, int width, int height, 
                             int rotationAngle, String pixelFormat) {
        if (frameBuffer == null) {
            throw new IllegalArgumentException("Frame buffer cannot be null");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive values");
        }
        if (rotationAngle % 90 != 0) {
            throw new IllegalArgumentException("Rotation angle must be a multiple of 90 degrees");
        }
        if (pixelFormat == null || pixelFormat.isEmpty()) {
            throw new IllegalArgumentException("Pixel format cannot be null or empty");
        }
        
        return nativeRotateFrame(frameBuffer, width, height, rotationAngle, pixelFormat);
    }
    
    /**
     * Rotate a video frame buffer in-place (modifies the original buffer)
     * @param frameBuffer Frame buffer to rotate (will be modified)
     * @param width Width of the video frame in pixels
     * @param height Height of the video frame in pixels
     * @param rotationAngle Rotation angle in degrees (90, 180, 270, etc.)
     * @return true if rotation was successful, false otherwise
     * @throws IllegalArgumentException if parameters are invalid
     */
    public boolean rotateFrameInPlace(byte[] frameBuffer, int width, int height, int rotationAngle) {
        if (frameBuffer == null) {
            throw new IllegalArgumentException("Frame buffer cannot be null");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive values");
        }
        if (rotationAngle % 90 != 0) {
            throw new IllegalArgumentException("Rotation angle must be a multiple of 90 degrees");
        }
        
        return nativeRotateFrameInPlace(frameBuffer, width, height, rotationAngle);
    }
    
    /**
     * Get the required buffer size for a rotated frame
     * @param width Width of the original frame in pixels
     * @param height Height of the original frame in pixels
     * @param pixelFormat Pixel format string
     * @return Required buffer size in bytes
     */
    public int getRequiredBufferSize(int width, int height, String pixelFormat) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive values");
        }
        if (pixelFormat == null || pixelFormat.isEmpty()) {
            throw new IllegalArgumentException("Pixel format cannot be null or empty");
        }
        
        return nativeGetRotatedBufferSize(width, height, pixelFormat);
    }
}