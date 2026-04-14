/**
 * CameraHardware
 *
 * Controls the physical camera shutter and focus subsystem by
 * delegating to the native HAL via {@link CameraApplication}.
 */
public class CameraHardware {

    // ----------------------------------------------------------------
    // Bounds on exposure time accepted by the native HAL
    // ----------------------------------------------------------------
    private static final int EXPOSURE_MIN_MS =    0;   // 0 = auto
    private static final int EXPOSURE_MAX_MS = 3000;   // 3-second long-exposure cap

    private final CameraApplication app;

    /** Index of the camera this instance controls. */
    private final int cameraId;

    /**
     * @param app       Shared {@link CameraApplication} bridge.
     * @param cameraId  0 = rear camera, 1 = front camera.
     */
    public CameraHardware(CameraApplication app, int cameraId) {
        if (app == null) {
            throw new IllegalArgumentException("CameraApplication must not be null.");
        }
        this.app      = app;
        this.cameraId = cameraId;
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Opens the shutter for the requested exposure duration.
     *
     * @param exposureTimeMs Shutter duration in ms; 0 = auto-exposure.
     * @return               true if the shutter opened successfully.
     */
    public boolean openShutter(int exposureTimeMs) {
        if (exposureTimeMs < EXPOSURE_MIN_MS || exposureTimeMs > EXPOSURE_MAX_MS) {
            throw new IllegalArgumentException(
                    "Exposure time must be in ["
                    + EXPOSURE_MIN_MS + ", " + EXPOSURE_MAX_MS
                    + "] ms, got: " + exposureTimeMs);
        }

        String label = (exposureTimeMs == 0) ? "AUTO" : exposureTimeMs + " ms";
        System.out.printf("[Camera %d] Opening shutter — exposure: %s%n",
                cameraId, label);

        // ── Native call ──────────────────────────────────────────────
        boolean ok = app.openShutter(cameraId, exposureTimeMs);
        // ────────────────────────────────────────────────────────────

        if (ok) {
            System.out.printf("[Camera %d] ✓ Shutter opened successfully.%n", cameraId);
        } else {
            System.err.printf("[Camera %d] ✗ ERROR: openShutter() rejected by hardware.%n",
                    cameraId);
        }
        return ok;
    }

    /**
     * Switches the camera to the given autofocus / manual-focus mode.
     *
     * @param focusMode One of the {@link CameraApplication.FocusMode} constants.
     * @param manualMm  Focus distance in mm; ignored unless
     *                  {@code focusMode == FocusMode.MANUAL}.
     * @return          true if the mode was accepted.
     */
    public boolean setFocusMode(int focusMode, float manualMm) {
        String modeLabel = focusModeLabel(focusMode);
        if (focusMode == CameraApplication.FocusMode.MANUAL) {
            System.out.printf("[Camera %d] Setting focus → MANUAL (%.1f mm)%n",
                    cameraId, manualMm);
        } else {
            System.out.printf("[Camera %d] Setting focus → %s%n", cameraId, modeLabel);
        }

        // ── Native call ──────────────────────────────────────────────
        boolean ok = app.setFocusMode(cameraId, focusMode, manualMm);
        // ────────────────────────────────────────────────────────────

        if (ok) {
            System.out.printf("[Camera %d] ✓ Focus mode set to %s.%n",
                    cameraId, modeLabel);
        } else {
            System.err.printf("[Camera %d] ✗ ERROR: setFocusMode() rejected by driver.%n",
                    cameraId);
        }
        return ok;
    }

    /**
     * Convenience: focus, then open the shutter.
     *
     * @param focusMode      Focus mode constant.
     * @param exposureTimeMs Shutter duration in ms; 0 = auto.
     * @return               true if both operations succeeded.
     */
    public boolean focusAndCapture(int focusMode, int exposureTimeMs) {
        System.out.printf("[Camera %d] === Focus & Capture ===%n", cameraId);
        boolean focused  = setFocusMode(focusMode, 0f);
        boolean captured = focused && openShutter(exposureTimeMs);
        System.out.printf("[Camera %d] === Capture %s ===%n",
                cameraId, captured ? "OK" : "FAILED");
        return captured;
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    private static String focusModeLabel(int mode) {
        switch (mode) {
            case CameraApplication.FocusMode.AUTO:     return "AUTO";
            case CameraApplication.FocusMode.MACRO:    return "MACRO";
            case CameraApplication.FocusMode.INFINITY: return "INFINITY";
            case CameraApplication.FocusMode.MANUAL:   return "MANUAL";
            default:                                   return "UNKNOWN(" + mode + ")";
        }
    }
}