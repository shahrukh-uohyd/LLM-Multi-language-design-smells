/**
 * HeartRateMonitor
 *
 * Acquires raw PPG samples from the optical sensor and derives
 * a calibrated BPM estimate via the native DSP pipeline.
 *
 * <p>All direct JNI calls are confined to this class; higher-level
 * wellness logic (zone detection, trend tracking) is handled above
 * this layer.
 */
public class HeartRateMonitor {

    // ----------------------------------------------------------------
    // Sensor and sampling configuration
    // ----------------------------------------------------------------
    /** Default PPG sensor index on this wearable hardware. */
    public static final int  DEFAULT_SENSOR_ID    = 0;
    /** ADC samples captured per measurement burst (1-second at 25 Hz). */
    public static final int  DEFAULT_SAMPLE_COUNT = 25;
    /** Sampling rate that matches the default sensor driver config (Hz). */
    public static final float DEFAULT_SAMPLING_HZ = 25.0f;

    // ----------------------------------------------------------------
    // BPM range used for physiological sanity checks
    // ----------------------------------------------------------------
    /** Minimum physiologically plausible BPM (resting athlete). */
    private static final float BPM_MIN = 30.0f;
    /** Maximum physiologically plausible BPM (peak exercise). */
    private static final float BPM_MAX = 220.0f;

    // ----------------------------------------------------------------
    // Heart-rate zone thresholds (percentage of 220-age max HR)
    // Applied here using fixed boundaries for simplicity.
    // ----------------------------------------------------------------
    private static final float ZONE_REST     = 60.0f;
    private static final float ZONE_FAT_BURN = 100.0f;
    private static final float ZONE_CARDIO   = 140.0f;
    private static final float ZONE_PEAK     = 170.0f;

    private final WearableBridge bridge;

    /**
     * @param bridge  Shared {@link WearableBridge} instance.
     */
    public HeartRateMonitor(WearableBridge bridge) {
        if (bridge == null) {
            throw new IllegalArgumentException("WearableBridge must not be null.");
        }
        this.bridge = bridge;
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Captures a PPG sample burst from the specified sensor.
     *
     * @param sensorId    Optical sensor index (0-based).
     * @param sampleCount Number of ADC samples to capture.
     * @return            Raw sample array, or {@code null} if the sensor
     *                    is unavailable.
     */
    public int[] readPpgSensor(int sensorId, int sampleCount) {
        if (sensorId < 0) {
            throw new IllegalArgumentException(
                "Sensor ID must be non-negative, got: " + sensorId);
        }
        if (sampleCount <= 0) {
            throw new IllegalArgumentException(
                "Sample count must be positive, got: " + sampleCount);
        }

        System.out.printf("[HRM] Capturing %d PPG samples from sensor %d...%n",
                sampleCount, sensorId);

        // ── Native call ──────────────────────────────────────────────
        int[] samples = bridge.readPpgSensor(sensorId, sampleCount);
        // ────────────────────────────────────────────────────────────

        if (samples == null) {
            System.err.printf("[HRM] ERROR: PPG sensor %d is unavailable.%n",
                    sensorId);
        } else {
            int min = samples[0], max = samples[0];
            for (int s : samples) {
                if (s < min) min = s;
                if (s > max) max = s;
            }
            System.out.printf("[HRM] ✓ %d samples captured. "
                    + "ADC range: [%d, %d]%n", samples.length, min, max);
        }
        return samples;
    }

    /**
     * Estimates BPM from a pre-captured PPG sample buffer.
     *
     * @param ppgSamples     Raw samples from {@link #readPpgSensor}.
     * @param samplingRateHz Sensor sampling frequency in Hz.
     * @return               Estimated BPM, or {@code -1.0f} on poor signal.
     */
    public float calculateBpm(int[] ppgSamples, float samplingRateHz) {
        if (ppgSamples == null || ppgSamples.length == 0) {
            throw new IllegalArgumentException(
                "PPG sample array must not be null or empty.");
        }
        if (samplingRateHz <= 0) {
            throw new IllegalArgumentException(
                "Sampling rate must be positive, got: " + samplingRateHz);
        }

        System.out.printf("[HRM] Calculating BPM from %d samples "
                + "@ %.1f Hz...%n", ppgSamples.length, samplingRateHz);

        // ── Native call ──────────────────────────────────────────────
        float bpm = bridge.calculateBpm(ppgSamples, samplingRateHz);
        // ────────────────────────────────────────────────────────────

        if (bpm < 0) {
            System.err.println("[HRM] ✗ Signal quality too poor for BPM estimate.");
        } else if (bpm < BPM_MIN || bpm > BPM_MAX) {
            System.err.printf("[HRM] ⚠  BPM %.1f outside plausible range "
                    + "[%.0f, %.0f] — discarding.%n", bpm, BPM_MIN, BPM_MAX);
            return -1.0f;
        } else {
            System.out.printf("[HRM] ✓ Heart rate: %.1f BPM — Zone: %s%n",
                    bpm, heartRateZone(bpm));
        }
        return bpm;
    }

    /**
     * Convenience: capture samples then immediately calculate BPM using
     * default sensor configuration.
     *
     * @return  Estimated BPM, or {@code -1.0f} on failure.
     */
    public float measureHeartRate() {
        System.out.println("[HRM] === One-shot Heart Rate Measurement ===");
        int[] samples = readPpgSensor(DEFAULT_SENSOR_ID, DEFAULT_SAMPLE_COUNT);
        if (samples == null) return -1.0f;
        return calculateBpm(samples, DEFAULT_SAMPLING_HZ);
    }

    /**
     * Performs multiple BPM measurements and returns the rolling average,
     * discarding any failed readings.
     *
     * @param readingCount  Number of individual measurements to average.
     * @return              Average BPM across valid readings,
     *                      or {@code -1.0f} if all readings failed.
     */
    public float measureAverageBpm(int readingCount) {
        if (readingCount <= 0) {
            throw new IllegalArgumentException(
                "Reading count must be positive, got: " + readingCount);
        }
        System.out.printf("[HRM] === Averaging over %d readings ===%n",
                readingCount);

        float sum   = 0;
        int   valid = 0;
        for (int i = 0; i < readingCount; i++) {
            float bpm = measureHeartRate();
            if (bpm > 0) { sum += bpm; valid++; }
        }

        if (valid == 0) {
            System.err.println("[HRM] No valid readings obtained.");
            return -1.0f;
        }
        float avg = sum / valid;
        System.out.printf("[HRM] Average BPM: %.1f "
                + "(%d/%d readings valid)%n", avg, valid, readingCount);
        return avg;
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    private static String heartRateZone(float bpm) {
        if (bpm < ZONE_REST)     return "RESTING";
        if (bpm < ZONE_FAT_BURN) return "FAT BURN";
        if (bpm < ZONE_CARDIO)   return "CARDIO";
        if (bpm < ZONE_PEAK)     return "PEAK";
        return "MAXIMUM";
    }
}