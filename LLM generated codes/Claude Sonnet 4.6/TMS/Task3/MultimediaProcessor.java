package com.multimedia.native_media;

/**
 * Central orchestrator that composes {@link AudioMetadataExtractor} and
 * {@link VideoFrameRotator} into a single, lifecycle-managed API.
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 *   MultimediaProcessor processor =
 *       new MultimediaProcessor(VideoFrameRotator.PixelFormat.RGBA_8888);
 *   processor.initialise();
 *
 *   // Audio
 *   AudioMetadata meta = processor.extractAudioMetadata("/media/track.flac");
 *   System.out.println(meta);
 *
 *   // Video
 *   byte[] rotated = processor.rotateVideoFrame(
 *       rawFrameBuffer, 1920, 1080,
 *       VideoFrameRotator.RotationAngle.ROTATE_90);
 * }</pre>
 */
public class MultimediaProcessor {

    private final AudioMetadataExtractor    audioExtractor;
    private final VideoFrameRotator         videoRotator;
    private final VideoFrameRotator.PixelFormat pixelFormat;

    private boolean initialised = false;

    /**
     * Creates a {@code MultimediaProcessor} that will handle video frames
     * in the specified pixel format.
     *
     * @param pixelFormat the pixel layout of video frames to be rotated;
     *                    must not be {@code null}
     */
    public MultimediaProcessor(VideoFrameRotator.PixelFormat pixelFormat) {
        if (pixelFormat == null) {
            throw new IllegalArgumentException("pixelFormat must not be null.");
        }
        this.pixelFormat    = pixelFormat;
        this.audioExtractor = new AudioMetadataExtractor();
        this.videoRotator   = new VideoFrameRotator();
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /**
     * Initialises all native components.
     * Must be called once at application startup before any other method.
     */
    public void initialise() {
        videoRotator.initRotationEngine(pixelFormat);
        initialised = true;
        System.out.printf("MultimediaProcessor initialised (pixelFormat=%s).%n", pixelFormat);
    }

    // ------------------------------------------------------------------
    // Audio
    // ------------------------------------------------------------------

    /**
     * Opens the audio stream at {@code streamUri}, extracts its metadata,
     * and returns it as an {@link AudioMetadata} object.
     *
     * @param streamUri absolute file path or network URI of the audio resource
     * @return a fully populated {@link AudioMetadata} instance
     * @throws IllegalStateException if {@link #initialise()} has not been called
     */
    public AudioMetadata extractAudioMetadata(String streamUri) {
        ensureInitialised();
        AudioMetadata metadata = audioExtractor.openAndExtract(streamUri);
        System.out.printf("Audio metadata extracted: %s%n", metadata);
        System.out.printf("Duration: %s%n", audioExtractor.formatDuration(metadata));
        return metadata;
    }

    // ------------------------------------------------------------------
    // Video
    // ------------------------------------------------------------------

    /**
     * Rotates a raw video frame buffer by the specified angle.
     *
     * @param frameBuffer raw pixel data of the source frame
     * @param width       frame width in pixels
     * @param height      frame height in pixels
     * @param angle       clockwise rotation to apply
     * @return a new {@code byte[]} containing the rotated pixel data
     * @throws IllegalStateException if {@link #initialise()} has not been called
     */
    public byte[] rotateVideoFrame(byte[]                        frameBuffer,
                                   int                           width,
                                   int                           height,
                                   VideoFrameRotator.RotationAngle angle) {
        ensureInitialised();
        byte[] rotated = videoRotator.rotateFrame(frameBuffer, width, height, angle);
        System.out.printf("Frame (%dx%d) rotated %d° → %d bytes returned.%n",
                width, height, angle.degrees(), rotated.length);
        return rotated;
    }

    // ------------------------------------------------------------------
    // Entry point (smoke test)
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        MultimediaProcessor processor =
                new MultimediaProcessor(VideoFrameRotator.PixelFormat.RGBA_8888);
        processor.initialise();

        // Audio metadata extraction
        AudioMetadata meta = processor.extractAudioMetadata("/media/audio/sample.flac");
        System.out.println(meta);

        // Video frame rotation
        int    width  = 1920;
        int    height = 1080;
        // 4 bytes per pixel for RGBA_8888
        byte[] rawFrame = new byte[width * height * 4];
        byte[] rotatedFrame = processor.rotateVideoFrame(
                rawFrame, width, height,
                VideoFrameRotator.RotationAngle.ROTATE_90);
        System.out.printf("Rotated frame size: %d bytes%n", rotatedFrame.length);
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private void ensureInitialised() {
        if (!initialised) {
            throw new IllegalStateException(
                "MultimediaProcessor has not been initialised. Call initialise() first.");
        }
    }
}