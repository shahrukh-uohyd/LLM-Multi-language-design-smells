/**
 * VisionSystem
 *
 * Provides barcode scanning, obstacle detection, and dense depth-map
 * computation by delegating to the native vision pipeline via
 * {@link RobotBridge}.
 *
 * <p>All image buffers are expected as raw RGBA byte arrays
 * (width × height × 4 bytes). The caller is responsible for
 * ensuring left/right stereo frames are captured at the same instant.
 */
public class VisionSystem {

    /** Minimum detection confidence to accept an obstacle report. */
    private static final float MIN_OBSTACLE_DISTANCE_M = 0.05f;  // 5 cm

    private final RobotBridge bridge;

    /**
     * @param bridge  Shared {@link RobotBridge} instance.
     */
    public VisionSystem(RobotBridge bridge) {
        if (bridge == null) {
            throw new IllegalArgumentException("RobotBridge must not be null.");
        }
        this.bridge = bridge;
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Attempts to scan and decode a barcode using the specified camera.
     *
     * @param cameraId   Scanner camera index (0-based).
     * @param timeoutMs  Decode timeout in milliseconds; 0 = single shot.
     * @return           Decoded barcode payload, or {@code null} if none found.
     */
    public String scanBarcode(int cameraId, int timeoutMs) {
        if (cameraId < 0) {
            throw new IllegalArgumentException(
                "Camera ID must be non-negative, got: " + cameraId);
        }
        if (timeoutMs < 0) {
            throw new IllegalArgumentException(
                "Timeout must be non-negative ms, got: " + timeoutMs);
        }

        System.out.printf("[Vision] Scanning barcode on camera %d "
                + "(timeout=%d ms)...%n", cameraId, timeoutMs);

        // ── Native call ──────────────────────────────────────────────
        String barcode = bridge.scanBarcode(cameraId, timeoutMs);
        // ────────────────────────────────────────────────────────────

        if (barcode == null) {
            System.out.println("[Vision] No barcode detected.");
        } else {
            System.out.printf("[Vision] ✓ Barcode decoded: '%s'%n", barcode);
        }
        return barcode;
    }

    /**
     * Queries the specified sensor array for the nearest obstacle.
     *
     * @param sensorArrayId  Sensor array index to query.
     * @return               {@link RobotBridge.ObstacleInfo} for the closest
     *                       obstacle, or {@code null} if the array is offline
     *                       or the field is clear.
     */
    public RobotBridge.ObstacleInfo detectObstacle(int sensorArrayId) {
        if (sensorArrayId < 0) {
            throw new IllegalArgumentException(
                "Sensor array ID must be non-negative, got: " + sensorArrayId);
        }

        System.out.printf("[Vision] Querying obstacle sensor array %d...%n",
                sensorArrayId);

        // ── Native call ──────────────────────────────────────────────
        RobotBridge.ObstacleInfo obstacle = bridge.detectObstacle(sensorArrayId);
        // ────────────────────────────────────────────────────────────

        if (obstacle == null) {
            System.out.println("[Vision] Sensor array offline or field clear.");
        } else {
            System.out.printf("[Vision] ✓ Obstacle detected: %s%n", obstacle);
            if (obstacle.distanceMetres < MIN_OBSTACLE_DISTANCE_M) {
                System.err.printf("[Vision] ⚠  PROXIMITY ALERT: obstacle at "
                        + "%.2f m — within minimum safe distance!%n",
                        obstacle.distanceMetres);
            }
        }
        return obstacle;
    }

    /**
     * Computes a dense depth map from a stereo camera frame pair.
     *
     * @param leftFrameRgba   Left  camera RGBA frame (w × h × 4 bytes).
     * @param rightFrameRgba  Right camera RGBA frame (w × h × 4 bytes).
     * @param frameWidth      Frame width  in pixels.
     * @param frameHeight     Frame height in pixels.
     * @return                Float array of per-pixel depths in metres
     *                        (length = w × h), or {@code null} on failure.
     */
    public float[] calculateDepthMap(byte[] leftFrameRgba,
                                     byte[] rightFrameRgba,
                                     int    frameWidth,
                                     int    frameHeight) {
        validateFrame(leftFrameRgba,  "leftFrameRgba",  frameWidth, frameHeight);
        validateFrame(rightFrameRgba, "rightFrameRgba", frameWidth, frameHeight);

        System.out.printf("[Vision] Computing depth map for %dx%d stereo pair...%n",
                frameWidth, frameHeight);

        // ── Native call ──────────────────────────────────────────────
        float[] depthMap = bridge.calculateDepthMap(
                leftFrameRgba, rightFrameRgba, frameWidth, frameHeight);
        // ────────────────────────────────────────────────────────────

        if (depthMap == null) {
            System.err.println("[Vision] ERROR: calculateDepthMap() returned null.");
        } else {
            float minDepth = Float.MAX_VALUE, maxDepth = -Float.MAX_VALUE;
            for (float d : depthMap) {
                if (d < minDepth) minDepth = d;
                if (d > maxDepth) maxDepth = d;
            }
            System.out.printf("[Vision] ✓ Depth map ready. "
                    + "Pixels=%d, range=[%.2f m, %.2f m]%n",
                    depthMap.length, minDepth, maxDepth);
        }
        return depthMap;
    }

    /**
     * Convenience: run obstacle detection on all sensor arrays in sequence.
     *
     * @param sensorCount  Total number of sensor arrays on this robot.
     * @return             Array of {@link RobotBridge.ObstacleInfo}, one per
     *                     array; entries are {@code null} for arrays that are
     *                     offline or report a clear field.
     */
    public RobotBridge.ObstacleInfo[] scanAllObstacleSensors(int sensorCount) {
        if (sensorCount <= 0) {
            throw new IllegalArgumentException(
                "Sensor count must be positive, got: " + sensorCount);
        }
        System.out.println("[Vision] === Full Obstacle Scan (" 
                + sensorCount + " arrays) ===");
        RobotBridge.ObstacleInfo[] results = new RobotBridge.ObstacleInfo[sensorCount];
        for (int i = 0; i < sensorCount; i++) {
            results[i] = detectObstacle(i);
        }
        System.out.println("[Vision] === Scan Complete ===");
        return results;
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    private static void validateFrame(byte[] frame, String name,
                                      int width, int height) {
        if (frame == null || frame.length == 0) {
            throw new IllegalArgumentException(
                name + " must not be null or empty.");
        }
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException(
                "Frame dimensions must be positive, got: "
                + width + "×" + height);
        }
        int expected = width * height * 4;
        if (frame.length != expected) {
            throw new IllegalArgumentException(
                name + " length mismatch: expected " + expected
                + " bytes for " + width + "×" + height
                + " RGBA frame, got " + frame.length);
        }
    }
}