import java.nio.ByteBuffer;

public class MultimediaNativeUtils {

    // Load the shared C++ multimedia library
    static {
        // Loads multimedia_core.dll (Windows), libmultimedia_core.so (Linux), or libmultimedia_core.dylib (macOS)
        System.loadLibrary("multimedia_core");
    }

    // ---------------------------------------------------------
    // 1. Audio Stream Metadata Extractor
    // ---------------------------------------------------------
    public static class AudioMetadataExtractor {
        /**
         * Parses an audio stream buffer in native C++ to extract metadata 
         * (e.g., ID3 tags for MP3, sample rate, bit depth, channels).
         *
         * @param audioStream A direct ByteBuffer containing the raw audio file stream.
         * @param streamSize  The number of valid bytes in the buffer.
         * @return A JSON-formatted String containing the extracted metadata properties,
         *         or null if extraction fails.
         */
        public native String extractMetadata(ByteBuffer audioStream, int streamSize);
    }

    // ---------------------------------------------------------
    // 2. Video Frame Rotator
    // ---------------------------------------------------------
    public static class VideoFrameProcessor {
        /**
         * Rotates a raw video frame buffer (e.g., YUV or RGB pixels) using native C++
         * (often leveraging SIMD instructions like AVX/NEON for speed).
         * 
         * By passing both a source and destination buffer, we avoid costly memory 
         * allocations inside the native layer or the JVM during a hot render loop.
         *
         * @param sourceFrame     A direct ByteBuffer containing the original video frame pixels.
         * @param destFrame       A direct ByteBuffer where the rotated pixels will be written.
         * @param width           The width of the original video frame.
         * @param height          The height of the original video frame.
         * @param rotationDegrees The angle of rotation (e.g., 90, 180, 270).
         */
        public native void rotateFrame(ByteBuffer sourceFrame, ByteBuffer destFrame, 
                                       int width, int height, int rotationDegrees);
    }

    // ---------------------------------------------------------
    // Example Usage
    // ---------------------------------------------------------
    public static void main(String[] args) {
        // 1. Audio Metadata Example
        AudioMetadataExtractor audioExtractor = new AudioMetadataExtractor();
        
        // Allocate a 1MB direct buffer for the audio stream chunk
        ByteBuffer audioBuffer = ByteBuffer.allocateDirect(1024 * 1024); 
        // (In a real app, you would read FileChannel or InputStream data into this buffer here)
        
        String metadataJson = audioExtractor.extractMetadata(audioBuffer, 512000); // Ex: 500KB read
        System.out.println("Audio Metadata: " + metadataJson);


        // 2. Video Frame Rotation Example (e.g., 1920x1080 RGBA frame)
        VideoFrameProcessor videoProcessor = new VideoFrameProcessor();
        
        int width = 1920;
        int height = 1080;
        int bytesPerPixel = 4; // RGBA
        
        // Allocate direct buffers for the source and destination frames
        ByteBuffer srcFrame = ByteBuffer.allocateDirect(width * height * bytesPerPixel);
        ByteBuffer dstFrame = ByteBuffer.allocateDirect(width * height * bytesPerPixel);
        
        // (In a real app, you would populate srcFrame with camera or decoder output here)

        // Rotate the frame 90 degrees clockwise
        videoProcessor.rotateFrame(srcFrame, dstFrame, width, height, 90);
        System.out.println("Video frame rotated successfully.");
    }
}