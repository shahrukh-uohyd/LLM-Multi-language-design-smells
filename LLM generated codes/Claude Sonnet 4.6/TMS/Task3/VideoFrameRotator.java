package com.multimedia.native_media;

/**
 * Provides native bindings for rotating a raw video frame buffer in-place.
 *
 * <p>The native layer is expected to operate directly on the pixel data
 * of a raw frame (e.g., RGBA8888 or YUV420) using SIMD-optimised routines
 * (NEON on ARM, SSE2/AVX2 on x86) for maximum throughput.</p>
 *
 * <p>Supported rotation angles:</p>
 * <ul>
 *   <li>{@link RotationAngle#ROTATE_90}  – 90° clockwise</li>
 *   <li>{@link RotationAngle#ROTATE_180} – 180° (vertical flip)</li>
 *   <li>{@link RotationAngle#ROTATE_270} – 270° clockwise (90° counter-clockwise)</li>
 * </ul>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 *   VideoFrameRotator rotator = new VideoFrameRotator();
 *   rotator.initRotationEngine(VideoFrameRotator.PixelFormat.RGBA_8888);
 *
 *   byte[] frameBuffer = captureRawFrame(); // width=1920, height=1080
 *   byte[] rotated = rotator.rotateFrame(
 *       frameBuffer,
 *       1920, 1080,
 *       VideoFrameRotator.RotationAngle.ROTATE_90.degrees()
 *   );
 * }</pre>
 *
 * <p><strong>Thread safety:</strong> A single instance is <em>not</em>
 * thread-safe. Create one instance per thread, or synchronise externally.</p>
 */
public class VideoFrameRotator {

    // ------------------------------------------------------------------
    // Enumerations used by the native interface
    // ------------------------------------------------------------------

    /**
     * Clockwise rotation angles supported by the native rotation engine.
     */
    public enum RotationAngle {

        /** Rotate 90 degrees clockwise. Output dimensions are swapped (W↔H). */
        ROTATE_90(90),

        /** Rotate 180 degrees. Output dimensions are identical to input. */
        ROTATE_180(180),

        /** Rotate 270 degrees clockwise (equivalent to 90° counter-clockwise).
         *  Output dimensions are swapped (W↔H). */
        ROTATE_270(270);

        private final int degrees;

        RotationAngle(int degrees) { this.degrees = degrees; }

        /** Returns the integer degree value expected by the native layer. */
        public int degrees() { return degrees; }
    }

    /**
     * Pixel format constants passed to {@link #initRotationEngine(PixelFormat)}.
     * The integer values match the native-layer enum and must not be changed
     * without a corresponding update in the C++ header.
     */
    public enum PixelFormat {

        /** 4 bytes per pixel — Red, Green, Blue, Alpha, 8 bits each. */
        RGBA_8888(0),

        /** 4 bytes per pixel — Blue, Green, Red, Alpha, 8 bits each. */
        BGRA_8888(1),

        /** Planar YUV 4:2:0 — 1.5 bytes per pixel. */
        YUV_420P(2),

        /** Semi-planar YUV 4:2:0 — Y plane + interleaved UV plane. */
        NV12(3);

        private final int nativeCode;

        PixelFormat(int nativeCode) { this.nativeCode = nativeCode; }

        /** Returns the integer code passed to the native initialisation routine. */
        public int nativeCode() { return nativeCode; }
    }

    // ------------------------------------------------------------------
    // Static initialiser
    // ------------------------------------------------------------------

    static {
        // Loads libvideo_rotator.so (Linux/macOS) or video_rotator.dll (Windows)
        System.loadLibrary("video_rotator");
    }

    // ------------------------------------------------------------------
    // Native method declarations
    // ------------------------------------------------------------------

    /**
     * Initialises the native rotation engine for a specific pixel format.
     *
     * <p>Must be called once before any {@link #rotateFrame} call.
     * Internally this pre-computes rotation index tables, allocates
     * any SIMD-aligned scratch buffers, and selects the optimised
     * code path for the given {@code pixelFormatCode}.</p>
     *
     * @param pixelFormatCode an integer identifying the pixel layout of
     *                        incoming frames; use {@link PixelFormat#nativeCode()}
     *                        to obtain the correct value
     * @throws IllegalArgumentException if {@code pixelFormatCode} is not
     *                                  one of the recognised format codes
     * @throws RuntimeException         if the native engine fails to
     *                                  allocate its internal resources
     */
    public native void initRotationEngine(int pixelFormatCode);

    /**
     * Rotates a raw video frame buffer and returns the rotated pixel data.
     *
     * <p>The input {@code frameBuffer} must contain exactly
     * {@code width × height × bytesPerPixel} bytes in row-major order
     * for the pixel format specified during {@link #initRotationEngine}.
     * The returned array has the same total byte count; for 90° and 270°
     * rotations the logical width and height are swapped in the output.</p>
     *
     * @param frameBuffer   raw pixel data of the source frame; must not be
     *                      {@code null} and must match
     *                      {@code width × height × bytesPerPixel} in length
     * @param width         width of the source frame in pixels; must be &gt; 0
     * @param height        height of the source frame in pixels; must be &gt; 0
     * @param rotationDegrees clockwise rotation in degrees; must be one of
     *                        90, 180, or 270 — use {@link RotationAngle#degrees()}
     * @return a new {@code byte[]} containing the rotated pixel data;
     *         never {@code null}
     * @throws IllegalStateException    if {@link #initRotationEngine(int)}
     *                                  has not been called first
     * @throws IllegalArgumentException if any dimension is &le; 0, the buffer
     *                                  size is inconsistent, or
     *                                  {@code rotationDegrees} is invalid
     * @throws RuntimeException         if the native rotation operation fails
     */
    public native byte[] rotateFrame(byte[] frameBuffer,
                                     int    width,
                                     int    height,
                                     int    rotationDegrees);

    // ------------------------------------------------------------------
    // Convenience overloads (pure Java — no JNI overhead)
    // ------------------------------------------------------------------

    /**
     * Typed overload of {@link #initRotationEngine(int)} that accepts a
     * {@link PixelFormat} enum value directly, improving call-site readability.
     *
     * @param format the pixel format of incoming frames; must not be {@code null}
     */
    public void initRotationEngine(PixelFormat format) {
        if (format == null) {
            throw new IllegalArgumentException("PixelFormat must not be null.");
        }
        initRotationEngine(format.nativeCode());
    }

    /**
     * Typed overload of {@link #rotateFrame} that accepts a {@link RotationAngle}
     * enum value instead of a raw integer.
     *
     * @param frameBuffer raw pixel data of the source frame
     * @param width       width of the source frame in pixels
     * @param height      height of the source frame in pixels
     * @param angle       clockwise rotation angle; must not be {@code null}
     * @return rotated pixel data as a new {@code byte[]}
     */
    public byte[] rotateFrame(byte[] frameBuffer,
                              int    width,
                              int    height,
                              RotationAngle angle) {
        if (angle == null) {
            throw new IllegalArgumentException("RotationAngle must not be null.");
        }
        return rotateFrame(frameBuffer, width, height, angle.degrees());
    }
}