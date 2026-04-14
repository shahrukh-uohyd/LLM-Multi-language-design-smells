/**
 * CameraApplication
 *
 * Central JNI bridge for the Camera Application.
 * Declares all 8 native methods across three functional groups
 * and loads the native shared library exactly once.
 *
 * Native library naming conventions:
 *   Linux   → libCameraApplication.so
 *   Windows → CameraApplication.dll
 *   macOS   → libCameraApplication.dylib
 *
 * To regenerate the C++ header:
 *   javac -h . CameraApplication.java
 */
public class CameraApplication {

    // ----------------------------------------------------------------
    // Static initializer — library is loaded once for the entire JVM.
    // ----------------------------------------------------------------
    static {
        System.loadLibrary("CameraApplication");
    }

    // ================================================================
    // GROUP 1 — Camera Hardware
    // Direct shutter and focus-mode control via the native HAL layer.
    // ================================================================

    /**
     * Opens the physical camera shutter to begin an exposure.
     *
     * @param cameraId       Hardware camera index (0 = rear, 1 = front).
     * @param exposureTimeMs Requested shutter-open duration in milliseconds.
     *                       Pass 0 for auto-exposure.
     * @return               true if the shutter opened successfully;
     *                       false if the hardware rejected the request.
     */
    public native boolean openShutter(int cameraId, int exposureTimeMs);

    /**
     * Sets the autofocus (or manual-focus) mode for the given camera.
     *
     * @param cameraId  Hardware camera index.
     * @param focusMode Focus strategy to apply. Valid constants:
     *                    {@link FocusMode#AUTO},
     *                    {@link FocusMode#MACRO},
     *                    {@link FocusMode#INFINITY},
     *                    {@link FocusMode#MANUAL}.
     * @param manualMm  Focus distance in millimetres; ignored unless
     *                  {@code focusMode == FocusMode.MANUAL}.
     * @return          true if the mode was accepted by the driver.
     */
    public native boolean setFocusMode(int cameraId, int focusMode, float manualMm);

    // ================================================================
    // GROUP 2 — GalleryPresenter
    // Image-processing operations performed on raw frame data.
    // ================================================================

    /**
     * Applies a sepia-tone colour transform to a raw RGBA image.
     *
     * @param rgbaPixels Raw RGBA pixel bytes (width × height × 4).
     * @param width      Image width in pixels.
     * @param height     Image height in pixels.
     * @return           New byte array containing the sepia-toned RGBA
     *                   pixels, or {@code null} if processing failed.
     */
    native byte[] applySepiaFilter(byte[] rgbaPixels, int width, int height);

    /**
     * Runs the native face-detection pipeline and returns bounding
     * rectangles for every face found in the image.
     *
     * @param rgbaPixels Raw RGBA pixel bytes.
     * @param width      Image width in pixels.
     * @param height     Image height in pixels.
     * @return           Array of {@link FaceRect} bounding boxes,
     *                   one per detected face; an empty array if none
     *                   are found; {@code null} on a hard failure.
     */
    native FaceRect[] detectFaceRectangles(byte[] rgbaPixels, int width, int height);

    /**
     * Encodes raw RGBA pixel data as a JPEG byte stream.
     *
     * @param rgbaPixels Raw RGBA pixel bytes.
     * @param width      Image width in pixels.
     * @param height     Image height in pixels.
     * @param quality    JPEG quality level [1 – 100].
     *                   Higher values mean better quality / larger files.
     * @return           Compressed JPEG bytes ready to write to disk
     *                   or transmit over the network; {@code null} on failure.
     */
    native byte[] compressToJpeg(byte[] rgbaPixels, int width, int height, int quality);

    // ================================================================
    // GROUP 3 — SharingUtility
    // Social upload, geo-tagging, and thumbnail generation.
    // ================================================================

    /**
     * Uploads a JPEG payload to the specified social-media endpoint.
     *
     * @param jpegBytes     Compressed JPEG bytes to upload.
     * @param platformToken Platform identifier token (e.g. "instagram",
     *                      "twitter") recognised by the native HTTP layer.
     * @param authToken     OAuth bearer token for the target platform.
     * @return              {@link UploadResult} describing the server's
     *                      response (URL, post ID, status code).
     */
    native UploadResult uploadToSocialMedia(byte[] jpegBytes,
                                            String platformToken,
                                            String authToken);

    /**
     * Writes EXIF GPS metadata into an in-memory JPEG buffer.
     *
     * @param jpegBytes  Existing JPEG bytes to tag (modified in place
     *                   by the native layer; a new buffer is returned).
     * @param latitude   GPS latitude  (decimal degrees, WGS-84).
     * @param longitude  GPS longitude (decimal degrees, WGS-84).
     * @param altitudeM  Altitude in metres above sea level.
     * @return           New JPEG byte array with GPS EXIF data embedded,
     *                   or {@code null} if tagging failed.
     */
    native byte[] tagGeoLocation(byte[] jpegBytes,
                                 double latitude,
                                 double longitude,
                                 double altitudeM);

    /**
     * Generates a scaled-down JPEG thumbnail from a full-resolution
     * JPEG source.
     *
     * @param jpegBytes     Full-resolution JPEG source bytes.
     * @param thumbWidth    Desired thumbnail width in pixels.
     * @param thumbHeight   Desired thumbnail height in pixels.
     * @return              Compressed thumbnail JPEG bytes,
     *                      or {@code null} on failure.
     */
    native byte[] generateThumbnail(byte[] jpegBytes,
                                    int thumbWidth,
                                    int thumbHeight);

    // ================================================================
    // Shared data types — returned by native methods above
    // ================================================================

    /**
     * Focus-mode constants passed to {@link #setFocusMode}.
     * Values must match the native enumeration in CameraApplication.h.
     */
    public static final class FocusMode {
        private FocusMode() {}

        /** Continuous autofocus — tracks the nearest subject. */
        public static final int AUTO     = 0;
        /** Close-range macro focus (< 20 cm). */
        public static final int MACRO    = 1;
        /** Fixed infinity focus — ideal for landscapes. */
        public static final int INFINITY = 2;
        /** Manual focus distance specified in millimetres. */
        public static final int MANUAL   = 3;
    }

    /**
     * Axis-aligned bounding rectangle returned by
     * {@link #detectFaceRectangles} for each detected face.
     */
    public static class FaceRect {
        /** Pixel x-coordinate of the rectangle's left edge. */
        public final int x;
        /** Pixel y-coordinate of the rectangle's top edge. */
        public final int y;
        /** Rectangle width in pixels. */
        public final int width;
        /** Rectangle height in pixels. */
        public final int height;
        /** Detection confidence score in [0.0, 1.0]. */
        public final float confidence;

        public FaceRect(int x, int y, int width, int height, float confidence) {
            this.x          = x;
            this.y          = y;
            this.width      = width;
            this.height     = height;
            this.confidence = confidence;
        }

        @Override
        public String toString() {
            return String.format(
                    "FaceRect{x=%d, y=%d, w=%d, h=%d, conf=%.2f}",
                    x, y, width, height, confidence);
        }
    }

    /**
     * Immutable result record returned by {@link #uploadToSocialMedia}.
     */
    public static class UploadResult {
        /** HTTP status code returned by the platform's API. */
        public final int    httpStatusCode;
        /** Platform-assigned post / media ID, or {@code null} on failure. */
        public final String postId;
        /** Publicly accessible URL of the uploaded image, or {@code null}. */
        public final String publicUrl;
        /** Human-readable status message from the native layer. */
        public final String statusMessage;

        public UploadResult(int    httpStatusCode,
                            String postId,
                            String publicUrl,
                            String statusMessage) {
            this.httpStatusCode = httpStatusCode;
            this.postId         = postId;
            this.publicUrl      = publicUrl;
            this.statusMessage  = statusMessage;
        }

        /** @return true if the HTTP status code indicates success (2xx). */
        public boolean isSuccess() {
            return httpStatusCode >= 200 && httpStatusCode < 300;
        }

        @Override
        public String toString() {
            return String.format(
                    "UploadResult{status=%d, postId='%s', url='%s', msg='%s'}",
                    httpStatusCode, postId, publicUrl, statusMessage);
        }
    }
}