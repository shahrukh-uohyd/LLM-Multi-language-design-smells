/**
 * SharingUtility
 *
 * Handles social-media upload, EXIF geo-tagging, and thumbnail
 * generation by delegating to the native layer via
 * {@link CameraApplication}.
 */
public class SharingUtility {

    // Thumbnail dimensions for social-media preview cards
    private static final int PREVIEW_THUMB_WIDTH  = 320;
    private static final int PREVIEW_THUMB_HEIGHT = 240;

    private final CameraApplication app;

    /**
     * @param app  Shared {@link CameraApplication} bridge.
     */
    public SharingUtility(CameraApplication app) {
        if (app == null) {
            throw new IllegalArgumentException("CameraApplication must not be null.");
        }
        this.app = app;
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Uploads a JPEG image to a social-media platform.
     *
     * @param jpegBytes     Compressed JPEG bytes to upload.
     * @param platformToken Platform identifier, e.g. {@code "instagram"}.
     * @param authToken     OAuth bearer token for the platform API.
     * @return              {@link CameraApplication.UploadResult}, or a
     *                      synthetic failure result on validation error.
     */
    public CameraApplication.UploadResult uploadToSocialMedia(byte[] jpegBytes,
                                                              String platformToken,
                                                              String authToken) {
        if (jpegBytes == null || jpegBytes.length == 0) {
            return failResult("JPEG bytes must not be null or empty.");
        }
        if (platformToken == null || platformToken.isBlank()) {
            return failResult("Platform token must not be null or blank.");
        }
        if (authToken == null || authToken.isBlank()) {
            return failResult("Auth token must not be null or blank.");
        }

        System.out.printf("[Sharing] Uploading %d-byte JPEG to '%s'...%n",
                jpegBytes.length, platformToken);

        // ── Native call ──────────────────────────────────────────────
        CameraApplication.UploadResult result =
                app.uploadToSocialMedia(jpegBytes, platformToken, authToken);
        // ────────────────────────────────────────────────────────────

        if (result == null) {
            System.err.println("[Sharing] ERROR: uploadToSocialMedia() returned null.");
            return failResult("Native upload returned null.");
        }

        if (result.isSuccess()) {
            System.out.printf("[Sharing] ✓ Upload successful. Post ID: '%s', "
                    + "URL: %s%n", result.postId, result.publicUrl);
        } else {
            System.err.printf("[Sharing] ✗ Upload FAILED (HTTP %d): %s%n",
                    result.httpStatusCode, result.statusMessage);
        }
        return result;
    }

    /**
     * Embeds GPS coordinates into a JPEG's EXIF metadata.
     *
     * @param jpegBytes  Source JPEG bytes.
     * @param latitude   Decimal-degree latitude  (WGS-84).
     * @param longitude  Decimal-degree longitude (WGS-84).
     * @param altitudeM  Altitude in metres above sea level.
     * @return           New JPEG bytes with GPS EXIF data, or
     *                   {@code null} on failure.
     */
    public byte[] tagGeoLocation(byte[] jpegBytes,
                                 double latitude,
                                 double longitude,
                                 double altitudeM) {
        if (jpegBytes == null || jpegBytes.length == 0) {
            throw new IllegalArgumentException(
                    "JPEG bytes must not be null or empty.");
        }
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException(
                    "Latitude must be in [-90, 90], got: " + latitude);
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException(
                    "Longitude must be in [-180, 180], got: " + longitude);
        }

        System.out.printf("[Sharing] Tagging JPEG with GPS: "
                + "lat=%.5f, lon=%.5f, alt=%.1f m...%n",
                latitude, longitude, altitudeM);

        // ── Native call ──────────────────────────────────────────────
        byte[] tagged = app.tagGeoLocation(jpegBytes, latitude, longitude, altitudeM);
        // ────────────────────────────────────────────────────────────

        if (tagged == null) {
            System.err.println("[Sharing] ERROR: tagGeoLocation() returned null.");
        } else {
            System.out.printf("[Sharing] ✓ Geo-tag applied. "
                    + "New JPEG size: %d bytes.%n", tagged.length);
        }
        return tagged;
    }

    /**
     * Generates a scaled-down JPEG thumbnail from a full-resolution source.
     *
     * @param jpegBytes    Full-resolution JPEG bytes.
     * @param thumbWidth   Desired thumbnail width in pixels.
     * @param thumbHeight  Desired thumbnail height in pixels.
     * @return             Thumbnail JPEG bytes, or {@code null} on failure.
     */
    public byte[] generateThumbnail(byte[] jpegBytes,
                                    int thumbWidth,
                                    int thumbHeight) {
        if (jpegBytes == null || jpegBytes.length == 0) {
            throw new IllegalArgumentException(
                    "JPEG bytes must not be null or empty.");
        }
        if (thumbWidth <= 0 || thumbHeight <= 0) {
            throw new IllegalArgumentException(
                    "Thumbnail dimensions must be positive, got: "
                    + thumbWidth + "x" + thumbHeight);
        }

        System.out.printf("[Sharing] Generating %dx%d thumbnail from %d-byte JPEG...%n",
                thumbWidth, thumbHeight, jpegBytes.length);

        // ── Native call ──────────────────────────────────────────────
        byte[] thumbnail = app.generateThumbnail(jpegBytes, thumbWidth, thumbHeight);
        // ────────────────────────────────────────────────────────────

        if (thumbnail == null) {
            System.err.println("[Sharing] ERROR: generateThumbnail() returned null.");
        } else {
            System.out.printf("[Sharing] ✓ Thumbnail generated. "
                    + "Size: %d bytes.%n", thumbnail.length);
        }
        return thumbnail;
    }

    /**
     * Convenience pipeline: geo-tag → generate preview thumbnail → upload.
     *
     * Steps:
     *  1. Embed GPS metadata into the JPEG.
     *  2. Derive a preview thumbnail at {@value #PREVIEW_THUMB_WIDTH}×
     *     {@value #PREVIEW_THUMB_HEIGHT}.
     *  3. Upload the full geo-tagged image to the platform.
     *
     * @param jpegBytes     Full-resolution JPEG bytes.
     * @param latitude      GPS latitude.
     * @param longitude     GPS longitude.
     * @param altitudeM     GPS altitude in metres.
     * @param platformToken Social-media platform identifier.
     * @param authToken     OAuth bearer token.
     * @return              Upload result from the platform API.
     */
    public CameraApplication.UploadResult geoTagAndShare(byte[] jpegBytes,
                                                         double latitude,
                                                         double longitude,
                                                         double altitudeM,
                                                         String platformToken,
                                                         String authToken) {
        System.out.println("[Sharing] === Geo-Tag & Share Pipeline ===");

        byte[] tagged = tagGeoLocation(jpegBytes, latitude, longitude, altitudeM);
        if (tagged == null) {
            return failResult("Geo-tagging failed; upload aborted.");
        }

        // Thumbnail is generated for local preview — not uploaded here.
        generateThumbnail(tagged, PREVIEW_THUMB_WIDTH, PREVIEW_THUMB_HEIGHT);

        CameraApplication.UploadResult result =
                uploadToSocialMedia(tagged, platformToken, authToken);

        System.out.println("[Sharing] === Pipeline Complete ===");
        return result;
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    private static CameraApplication.UploadResult failResult(String message) {
        System.err.println("[Sharing] ✗ " + message);
        return new CameraApplication.UploadResult(0, null, null, message);
    }
}