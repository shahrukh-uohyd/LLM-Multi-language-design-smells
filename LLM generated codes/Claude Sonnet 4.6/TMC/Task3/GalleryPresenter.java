import java.util.Arrays;

/**
 * GalleryPresenter
 *
 * Applies image-processing operations to raw camera frames:
 * sepia filtering, face detection, and JPEG compression.
 * All heavy lifting is delegated to the native layer via
 * {@link CameraApplication}.
 */
public class GalleryPresenter {

    // ----------------------------------------------------------------
    // JPEG quality presets
    // ----------------------------------------------------------------
    public static final int JPEG_QUALITY_LOW    = 50;
    public static final int JPEG_QUALITY_MEDIUM = 75;
    public static final int JPEG_QUALITY_HIGH   = 90;

    private final CameraApplication app;

    /**
     * @param app  Shared {@link CameraApplication} bridge.
     */
    public GalleryPresenter(CameraApplication app) {
        if (app == null) {
            throw new IllegalArgumentException("CameraApplication must not be null.");
        }
        this.app = app;
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Applies a sepia-tone filter to raw RGBA pixel data.
     *
     * @param rgbaPixels Raw RGBA byte array (width × height × 4 bytes).
     * @param width      Image width in pixels.
     * @param height     Image height in pixels.
     * @return           Sepia-toned RGBA byte array, or {@code null} on failure.
     */
    public byte[] applySepia(byte[] rgbaPixels, int width, int height) {
        validateImageArgs(rgbaPixels, width, height);

        System.out.printf("[Gallery] Applying sepia filter to %dx%d image...%n",
                width, height);

        // ── Native call ──────────────────────────────────────────────
        byte[] result = app.applySepiaFilter(rgbaPixels, width, height);
        // ────────────────────────────────────────────────────────────

        if (result == null) {
            System.err.println("[Gallery] ERROR: applySepiaFilter() returned null.");
        } else {
            System.out.printf("[Gallery] ✓ Sepia filter applied. "
                    + "Output size: %d bytes.%n", result.length);
        }
        return result;
    }

    /**
     * Detects all faces in a raw RGBA image and returns their
     * bounding rectangles.
     *
     * @param rgbaPixels Raw RGBA byte array.
     * @param width      Image width in pixels.
     * @param height     Image height in pixels.
     * @return           Array of {@link CameraApplication.FaceRect};
     *                   never {@code null} — returns an empty array
     *                   if no faces are found.
     */
    public CameraApplication.FaceRect[] detectFaces(byte[] rgbaPixels,
                                                    int width, int height) {
        validateImageArgs(rgbaPixels, width, height);

        System.out.printf("[Gallery] Running face detection on %dx%d image...%n",
                width, height);

        // ── Native call ──────────────────────────────────────────────
        CameraApplication.FaceRect[] faces =
                app.detectFaceRectangles(rgbaPixels, width, height);
        // ────────────────────────────────────────────────────────────

        if (faces == null) {
            System.err.println("[Gallery] ERROR: detectFaceRectangles() failed.");
            return new CameraApplication.FaceRect[0];
        }

        if (faces.length == 0) {
            System.out.println("[Gallery] No faces detected.");
        } else {
            System.out.println("[Gallery] Detected " + faces.length + " face(s):");
            for (int i = 0; i < faces.length; i++) {
                System.out.printf("[Gallery]   [%d] %s%n", i + 1, faces[i]);
            }
        }
        return faces;
    }

    /**
     * Compresses raw RGBA data to a JPEG byte stream.
     *
     * @param rgbaPixels Raw RGBA byte array.
     * @param width      Image width in pixels.
     * @param height     Image height in pixels.
     * @param quality    JPEG quality [1 – 100].
     * @return           Compressed JPEG bytes, or {@code null} on failure.
     */
    public byte[] compressToJpeg(byte[] rgbaPixels, int width, int height,
                                 int quality) {
        validateImageArgs(rgbaPixels, width, height);
        if (quality < 1 || quality > 100) {
            throw new IllegalArgumentException(
                    "JPEG quality must be in [1, 100], got: " + quality);
        }

        System.out.printf("[Gallery] Compressing %dx%d image to JPEG "
                + "(quality=%d)...%n", width, height, quality);

        // ── Native call ──────────────────────────────────────────────
        byte[] jpeg = app.compressToJpeg(rgbaPixels, width, height, quality);
        // ────────────────────────────────────────────────────────────

        if (jpeg == null) {
            System.err.println("[Gallery] ERROR: compressToJpeg() returned null.");
        } else {
            System.out.printf("[Gallery] ✓ JPEG compression complete. "
                    + "Output: %d bytes (%.1f%% of raw).%n",
                    jpeg.length,
                    100.0 * jpeg.length / rgbaPixels.length);
        }
        return jpeg;
    }

    /**
     * Convenience pipeline: sepia → face detection → JPEG compression.
     * Useful for a single-call "process and save" workflow.
     *
     * @param rgbaPixels Raw RGBA source pixels.
     * @param width      Image width.
     * @param height     Image height.
     * @param quality    JPEG quality for the final output.
     * @return           JPEG bytes of the sepia-toned image,
     *                   or {@code null} if any stage fails.
     */
    public byte[] processCapturedFrame(byte[] rgbaPixels, int width,
                                       int height, int quality) {
        System.out.println("[Gallery] === Full Processing Pipeline ===");

        byte[] sepia = applySepia(rgbaPixels, width, height);
        if (sepia == null) return null;

        detectFaces(sepia, width, height);        // informational — result logged

        byte[] jpeg = compressToJpeg(sepia, width, height, quality);
        System.out.println("[Gallery] === Pipeline Complete ===");
        return jpeg;
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    private static void validateImageArgs(byte[] pixels, int width, int height) {
        if (pixels == null || pixels.length == 0) {
            throw new IllegalArgumentException("Pixel array must not be null or empty.");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                    "Image dimensions must be positive, got: "
                    + width + "x" + height);
        }
        int expected = width * height * 4;          // RGBA = 4 bytes per pixel
        if (pixels.length != expected) {
            throw new IllegalArgumentException(
                    "Pixel array length mismatch: expected " + expected
                    + " bytes for " + width + "x" + height
                    + " RGBA image, got " + pixels.length);
        }
    }
}