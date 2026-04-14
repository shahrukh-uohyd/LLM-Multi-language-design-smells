import java.util.Arrays;

/**
 * MediaCenter
 *
 * Controls native audio playback and DSP equalizer settings
 * through InfotainmentBridge.
 *
 * EQ band layout (5-band graphic EQ):
 *   Band 0 →  60 Hz  (Sub-bass)
 *   Band 1 → 250 Hz  (Bass)
 *   Band 2 →   1 kHz (Midrange)
 *   Band 3 →   4 kHz (Upper-mid)
 *   Band 4 →  16 kHz (Presence / Air)
 */
public class MediaCenter {

    /** Number of EQ bands supported by the native DSP. */
    public static final int EQ_BAND_COUNT = 5;

    /** Flat (neutral) EQ preset — no boost or cut on any band. */
    public static final float[] EQ_PRESET_FLAT =
            new float[]{0f, 0f, 0f, 0f, 0f};

    /** Bass-boost preset — enhanced low-frequency response. */
    public static final float[] EQ_PRESET_BASS_BOOST =
            new float[]{6f, 4f, 0f, -1f, -2f};

    /** Vocal-clarity preset — cuts bass, boosts upper-mids. */
    public static final float[] EQ_PRESET_VOCAL =
            new float[]{-3f, -1f, 2f, 4f, 3f};

    private final InfotainmentBridge bridge;

    /** Handle of the currently active stream, or -1 if none. */
    private int activeStreamHandle = -1;

    /**
     * @param bridge  Shared InfotainmentBridge instance.
     */
    public MediaCenter(InfotainmentBridge bridge) {
        if (bridge == null) {
            throw new IllegalArgumentException("InfotainmentBridge must not be null.");
        }
        this.bridge = bridge;
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Starts streaming an MP3 source at the given volume.
     *
     * @param streamUri    URI of the MP3 resource.
     * @param volumeLevel  Initial volume [0 – 100].
     * @return             Stream handle, or -1 on failure.
     */
    public int startStream(String streamUri, int volumeLevel) {
        if (streamUri == null || streamUri.isBlank()) {
            throw new IllegalArgumentException("Stream URI must not be null or blank.");
        }
        if (volumeLevel < 0 || volumeLevel > 100) {
            throw new IllegalArgumentException(
                    "Volume must be in range [0, 100], got: " + volumeLevel);
        }

        System.out.printf("[Media] Opening MP3 stream: '%s' at volume %d...%n",
                streamUri, volumeLevel);

        // ── Native call ──────────────────────────────────────────────
        int handle = bridge.playMp3Stream(streamUri, volumeLevel);
        // ────────────────────────────────────────────────────────────

        if (handle < 0) {
            System.err.println("[Media] ERROR: Failed to open stream '" + streamUri + "'.");
        } else {
            activeStreamHandle = handle;
            System.out.println("[Media] Stream opened. Handle=" + handle);
        }
        return handle;
    }

    /**
     * Applies a 5-band EQ preset to the currently active stream.
     *
     * @param preset  EQ gain array; must have exactly {@value #EQ_BAND_COUNT} elements.
     * @return        true if the EQ was applied successfully.
     */
    public boolean applyEqPreset(float[] preset) {
        requireActiveStream("applyEqPreset");

        if (preset == null || preset.length != EQ_BAND_COUNT) {
            throw new IllegalArgumentException(
                    "EQ preset must have exactly " + EQ_BAND_COUNT
                    + " bands, got: "
                    + (preset == null ? "null" : preset.length));
        }

        System.out.printf("[Media] Applying EQ bands %s to stream handle %d...%n",
                Arrays.toString(preset), activeStreamHandle);

        // ── Native call ──────────────────────────────────────────────
        boolean applied = bridge.adjustEqualizer(activeStreamHandle, preset);
        // ────────────────────────────────────────────────────────────

        if (applied) {
            System.out.println("[Media] EQ applied successfully.");
        } else {
            System.err.println("[Media] ERROR: adjustEqualizer() failed.");
        }
        return applied;
    }

    /**
     * Convenience: start a stream and immediately apply a preset.
     *
     * @param streamUri    MP3 source URI.
     * @param volumeLevel  Initial volume [0 – 100].
     * @param eqPreset     5-band EQ preset to apply after opening.
     * @return             Stream handle, or -1 on failure.
     */
    public int startStreamWithPreset(String streamUri,
                                     int    volumeLevel,
                                     float[] eqPreset) {
        int handle = startStream(streamUri, volumeLevel);
        if (handle >= 0) {
            applyEqPreset(eqPreset);
        }
        return handle;
    }

    /** @return The handle of the currently active stream, or -1 if none. */
    public int getActiveStreamHandle() {
        return activeStreamHandle;
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    private void requireActiveStream(String callerName) {
        if (activeStreamHandle < 0) {
            throw new IllegalStateException(
                    "[Media] " + callerName + "() called with no active stream. "
                    + "Call startStream() first.");
        }
    }
}