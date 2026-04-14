package com.app.sensor;

import com.app.signal.SignalProcessingException;
import com.app.signal.SignalProcessorNative;
import org.junit.jupiter.api.*;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link SensorAnalyzer} and {@link SignalProcessorNative}.
 * Exercises the full Java → JNI → C++ path.
 *
 * Build the native library first, then run with:
 *   mvn test  -Djava.library.path=lib
 *   gradle test (configure nativeLibraryPath in build.gradle)
 */
class SensorAnalyzerTest {

    private SignalProcessorNative dsp;

    @BeforeEach
    void setUp() { dsp = new SignalProcessorNative(); }

    // ── FFT round-trip ────────────────────────────────────────────────

    @Test
    void fftIFFT_roundTrip_restoresOriginal() {
        double[] original = generateSineWave(1024, 10.0, 1024.0); // 10 Hz sine
        double[] spectrum  = dsp.computeFFT(original, SignalProcessorNative.WINDOW_RECTANGULAR);
        double[] recovered = dsp.computeIFFT(spectrum);

        assertEquals(original.length, recovered.length);
        for (int i = 0; i < original.length; i++) {
            assertEquals(original[i], recovered[i], 1e-9,
                "IFFT(FFT(x)) should restore x at index " + i);
        }
    }

    @Test
    void fft_lengthIsDoubleInput() {
        double[] sig = new double[512];
        double[] out = dsp.computeFFT(sig, SignalProcessorNative.WINDOW_HANN);
        assertEquals(1024, out.length, "FFT output should be 2× input (interleaved)");
    }

    @Test
    void fft_nonPowerOfTwoThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> dsp.computeFFT(new double[100], SignalProcessorNative.WINDOW_RECTANGULAR));
    }

    @Test
    void fft_allWindowTypesSucceed() {
        double[] sig = generateSineWave(256, 5.0, 256.0);
        for (int w = 0; w <= 3; w++) {
            int window = w;
            assertDoesNotThrow(() -> dsp.computeFFT(sig, window),
                "Window type " + w + " should not throw");
        }
    }

    // ── dominant frequency detection ──────────────────────────────────

    @Test
    void dominantFrequency_detectedAccurately() {
        double sampleRate = 1024.0;
        double targetFreq = 64.0;
        double[] sig      = generateSineWave(1024, targetFreq, sampleRate);
        double[] spectrum  = dsp.computeFFT(sig, SignalProcessorNative.WINDOW_RECTANGULAR);
        double[] mag       = dsp.computeMagnitudeSpectrum(spectrum);
        double   dominant  = dsp.findDominantFrequency(mag, sampleRate);

        assertEquals(targetFreq, dominant,
            sampleRate / sig.length + 1.0, // tolerance = freq resolution + 1 Hz
            "Dominant frequency should be close to " + targetFreq + " Hz");
    }

    // ── FIR filter ────────────────────────────────────────────────────

    @Test
    void firFilter_preservesSameLength() {
        double[] sig    = generateSineWave(512, 10.0, 512.0);
        double[] coeffs = {0.25, 0.5, 0.25}; // simple smoothing kernel
        double[] out    = dsp.applyFIRFilter(sig, coeffs);
        assertEquals(sig.length, out.length);
    }

    @Test
    void firFilter_lowPassAttenuatesHighFreq() {
        // Low-pass should reduce the magnitude of a high-frequency signal
        double[] highFreq = generateSineWave(1024, 400.0, 1024.0);
        double[] lpCoeffs = {-0.0106, 0.0, 0.3194, 0.5, 0.3194, 0.0, -0.0106};
        double[] filtered = dsp.applyFIRFilter(highFreq, lpCoeffs);
        double rmsBefore  = rms(highFreq);
        double rmsAfter   = rms(filtered);
        assertTrue(rmsAfter < rmsBefore * 0.5,
            "Low-pass filter should attenuate high-frequency content");
    }

    // ── IIR filter ────────────────────────────────────────────────────

    @Test
    void iirFilter_preservesSameLength() {
        double[] sig = generateSineWave(256, 50.0, 1024.0);
        double[] bq  = {0.9969, -1.9938, 0.9969, -1.9937, 0.9939};
        assertEquals(sig.length, dsp.applyIIRFilter(sig, bq).length);
    }

    @Test
    void iirFilter_wrongCoeffCountThrows() {
        double[] sig = new double[]{1.0, 2.0, 3.0};
        assertThrows(IllegalArgumentException.class,
            () -> dsp.applyIIRFilter(sig, new double[]{1.0, 2.0, 3.0}));
    }

    // ── resampling ────────────────────────────────────────────────────

    @Test
    void resample_upsampleDoublesLength() {
        double[] sig = generateSineWave(512, 10.0, 512.0);
        double[] up  = dsp.resample(sig, 512.0, 1024.0);
        assertEquals(sig.length * 2, up.length, 2, "2× upsampling should ~double the length");
    }

    @Test
    void resample_downsampleHalvesLength() {
        double[] sig  = generateSineWave(1024, 10.0, 1024.0);
        double[] down = dsp.resample(sig, 1024.0, 512.0);
        assertEquals(sig.length / 2, down.length, 2, "2× downsampling should ~halve the length");
    }

    @Test
    void resample_sameRateReturnsSameLength() {
        double[] sig = new double[]{1.0, 2.0, 3.0, 4.0};
        assertEquals(sig.length, dsp.resample(sig, 1000.0, 1000.0).length);
    }

    // ── statistics ────────────────────────────────────────────────────

    @Test
    void statistics_pureZeroSignal() {
        double[] zeros = new double[256];
        double[] stats = dsp.computeStatistics(zeros);
        assertEquals(SignalProcessorNative.STATS_ARRAY_LENGTH, stats.length);
        assertEquals(0.0, stats[SignalProcessorNative.STATS_IDX_MEAN],     1e-12);
        assertEquals(0.0, stats[SignalProcessorNative.STATS_IDX_VARIANCE], 1e-12);
        assertEquals(0.0, stats[SignalProcessorNative.STATS_IDX_RMS],      1e-12);
    }

    @Test
    void statistics_knownValues() {
        double[] sig = {1.0, 2.0, 3.0, 4.0, 5.0};
        double[] s   = dsp.computeStatistics(sig);
        assertEquals(3.0,  s[SignalProcessorNative.STATS_IDX_MEAN],    1e-9);
        assertEquals(2.5,  s[SignalProcessorNative.STATS_IDX_VARIANCE], 1e-9);
        assertEquals(1.0,  s[SignalProcessorNative.STATS_IDX_MIN],     1e-9);
        assertEquals(5.0,  s[SignalProcessorNative.STATS_IDX_MAX],     1e-9);
    }

    @Test
    void statistics_zeroCrossingRate_squareWave() {
        // Alternating +1 / -1 → every sample is a crossing
        double[] sq = new double[100];
        for (int i = 0; i < sq.length; i++) sq[i] = (i % 2 == 0) ? 1.0 : -1.0;
        double[] s = dsp.computeStatistics(sq);
        assertEquals(1.0, s[SignalProcessorNative.STATS_IDX_ZCR], 0.02);
    }

    // ── convolution ───────────────────────────────────────────────────

    @Test
    void convolve_outputLength() {
        double[] sig = {1, 2, 3, 4, 5};
        double[] ker = {1, 0, -1};
        double[] out = dsp.convolve(sig, ker);
        assertEquals(sig.length + ker.length - 1, out.length);
    }

    @Test
    void convolve_identityKernel() {
        double[] sig = {1.0, 2.0, 3.0, 4.0, 5.0};
        double[] id  = {1.0};
        double[] out = dsp.convolve(sig, id);
        assertArrayEquals(sig, out, 1e-12);
    }

    // ── magnitude spectrum ────────────────────────────────────────────

    @Test
    void magnitudeSpectrum_isHalfComplexLength() {
        double[] sig  = new double[256];
        double[] spec = dsp.computeFFT(sig, SignalProcessorNative.WINDOW_RECTANGULAR);
        double[] mag  = dsp.computeMagnitudeSpectrum(spec);
        assertEquals(spec.length / 2, mag.length);
    }

    @Test
    void magnitudeSpectrum_allNonNegative() {
        double[] sig  = generateSineWave(512, 20.0, 512.0);
        double[] spec = dsp.computeFFT(sig, SignalProcessorNative.WINDOW_HANN);
        double[] mag  = dsp.computeMagnitudeSpectrum(spec);
        for (double v : mag) assertTrue(v >= 0.0, "magnitude must be non-negative");
    }

    // ── error handling ────────────────────────────────────────────────

    @Test
    void computeFFT_nullThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> dsp.computeFFT(null, 0));
    }

    @Test
    void computeStatistics_emptyThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> dsp.computeStatistics(new double[0]));
    }

    @Test
    void resample_negativeRateThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> dsp.resample(new double[]{1.0}, -1.0, 100.0));
    }

    // ── helpers ───────────────────────────────────────────────────────

    /** Generates a real-valued sine wave at {@code freqHz} Hz. */
    private static double[] generateSineWave(int samples, double freqHz, double sampleRate) {
        double[] sig = new double[samples];
        for (int i = 0; i < samples; i++) {
            sig[i] = Math.sin(2.0 * Math.PI * freqHz * i / sampleRate);
        }
        return sig;
    }

    /** Root-mean-square of an array. */
    private static double rms(double[] x) {
        double sum = 0;
        for (double v : x) sum += v * v;
        return Math.sqrt(sum / x.length);
    }
}