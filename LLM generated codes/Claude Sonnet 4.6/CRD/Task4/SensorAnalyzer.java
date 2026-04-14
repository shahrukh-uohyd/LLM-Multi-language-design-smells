package com.app.sensor;

import com.app.device.DeviceController;
import com.app.signal.SignalProcessingException;
import com.app.signal.SignalProcessorNative;

import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Analyzes raw sensor data acquired from hardware via {@link DeviceController}.
 *
 * <p>This class is the primary consumer of {@link SignalProcessorNative}.
 * It is responsible for:</p>
 * <ul>
 *   <li>Acquiring raw samples from the device</li>
 *   <li>Pre-processing (resampling to a canonical rate)</li>
 *   <li>Spectral analysis via FFT</li>
 *   <li>Noise reduction via FIR / IIR filtering</li>
 *   <li>Statistical feature extraction</li>
 *   <li>Dominant-frequency detection</li>
 *   <li>Providing a fully processed {@link SensorAnalysisResult} to callers</li>
 * </ul>
 *
 * <h2>Typical usage</h2>
 * <pre>{@code
 *   DeviceController  hw       = new DeviceController();
 *   SensorAnalyzer    analyzer = new SensorAnalyzer(hw, 1024.0);
 *
 *   int handle = hw.openDevice("sensor-01");
 *   SensorAnalysisResult result = analyzer.analyze(handle, 2048);
 *
 *   System.out.println("Dominant freq: " + result.getDominantFrequencyHz() + " Hz");
 *   System.out.println("RMS amplitude: " + result.getRms());
 * }</pre>
 */
public class SensorAnalyzer {

    private static final Logger LOG = Logger.getLogger(SensorAnalyzer.class.getName());

    /** Canonical internal sample rate to which all raw input is normalised. */
    public static final double CANONICAL_SAMPLE_RATE_HZ = 1024.0;

    /** Default FFT window size (power of two). */
    public static final int DEFAULT_FFT_WINDOW = 1024;

    /**
     * Low-pass FIR anti-aliasing filter coefficients (7-tap Hann-windowed sinc,
     * cut-off ≈ 0.4 × Nyquist). Replace with application-specific coefficients
     * for production use.
     */
    private static final double[] LOWPASS_FIR_COEFFS = {
        -0.0106, 0.0, 0.3194, 0.5, 0.3194, 0.0, -0.0106
    };

    /**
     * Biquad IIR high-pass coefficients {@code [b0,b1,b2,a1,a2]} — removes DC offset.
     * Butterworth 2nd-order, cut-off ≈ 1 Hz @ 1024 Hz sample rate.
     */
    private static final double[] HIGHPASS_IIR_COEFFS = {
        0.9969, -1.9938,  0.9969,   // b0 b1 b2
        -1.9937,  0.9939             // a1 a2
    };

    // ── fields ────────────────────────────────────────────────────────

    private final DeviceController        deviceController;
    private final SignalProcessorNative   signalProcessor;
    private final double                  inputSampleRateHz;
    private final int                     fftWindowSize;

    // ── constructors ─────────────────────────────────────────────────

    /**
     * Creates a {@code SensorAnalyzer} with the default FFT window size.
     *
     * @param deviceController hardware interface (must not be null)
     * @param inputSampleRateHz sample rate of the raw device output in Hz
     */
    public SensorAnalyzer(DeviceController deviceController, double inputSampleRateHz) {
        this(deviceController, inputSampleRateHz,
             DEFAULT_FFT_WINDOW, new SignalProcessorNative());
    }

    /**
     * Creates a {@code SensorAnalyzer} with a custom FFT window size.
     *
     * @param deviceController  hardware interface (must not be null)
     * @param inputSampleRateHz sample rate of the raw device output in Hz
     * @param fftWindowSize     FFT window size — must be a power of two
     */
    public SensorAnalyzer(DeviceController deviceController,
                          double inputSampleRateHz,
                          int fftWindowSize) {
        this(deviceController, inputSampleRateHz, fftWindowSize, new SignalProcessorNative());
    }

    /**
     * Full constructor — package-accessible for testing with injected dependencies.
     */
    SensorAnalyzer(DeviceController deviceController,
                   double inputSampleRateHz,
                   int fftWindowSize,
                   SignalProcessorNative signalProcessor) {
        this.deviceController  = Objects.requireNonNull(deviceController);
        this.signalProcessor   = Objects.requireNonNull(signalProcessor);
        SignalProcessorNative.checkSampleRate(inputSampleRateHz, "inputSampleRateHz");
        SignalProcessorNative.checkPowerOfTwo(fftWindowSize, "fftWindowSize");
        this.inputSampleRateHz = inputSampleRateHz;
        this.fftWindowSize     = fftWindowSize;

        LOG.info("SensorAnalyzer ready — inputRate=" + inputSampleRateHz
            + " Hz, fftWindow=" + fftWindowSize);
    }

    // ── public API ────────────────────────────────────────────────────

    /**
     * Acquires {@code sampleCount} raw samples from the device on {@code deviceHandle},
     * runs the full DSP pipeline, and returns a {@link SensorAnalysisResult}.
     *
     * <p>Pipeline steps:</p>
     * <ol>
     *   <li>Read raw samples from device hardware</li>
     *   <li>Resample to canonical rate (if input rate ≠ canonical)</li>
     *   <li>Apply high-pass IIR filter (DC removal)</li>
     *   <li>Apply low-pass FIR anti-aliasing filter</li>
     *   <li>Extract statistical features</li>
     *   <li>Apply Hann-windowed FFT</li>
     *   <li>Compute magnitude spectrum</li>
     *   <li>Detect dominant frequency</li>
     * </ol>
     *
     * @param deviceHandle device handle from {@link DeviceController#openDevice}
     * @param sampleCount  number of raw samples to read (must be &gt; 0)
     * @return             fully populated {@link SensorAnalysisResult}
     * @throws IllegalArgumentException  if sampleCount &lt;= 0
     * @throws SignalProcessingException if any DSP step fails
     */
    public SensorAnalysisResult analyze(int deviceHandle, int sampleCount) {
        if (sampleCount <= 0) {
            throw new IllegalArgumentException("sampleCount must be > 0, got: " + sampleCount);
        }

        // ── Step 1: acquire raw samples from hardware ─────────────────
        double[] raw = acquireRawSamples(deviceHandle, sampleCount);
        LOG.fine("Acquired " + raw.length + " raw samples");

        // ── Step 2: resample to canonical rate ────────────────────────
        double[] resampled = raw;
        if (Double.compare(inputSampleRateHz, CANONICAL_SAMPLE_RATE_HZ) != 0) {
            resampled = signalProcessor.resample(raw, inputSampleRateHz,
                                                 CANONICAL_SAMPLE_RATE_HZ);
            LOG.fine("Resampled " + raw.length + " → " + resampled.length + " samples");
        }

        // ── Step 3: high-pass IIR to remove DC offset ─────────────────
        double[] dcRemoved = signalProcessor.applyIIRFilter(resampled, HIGHPASS_IIR_COEFFS);

        // ── Step 4: low-pass FIR anti-aliasing filter ─────────────────
        double[] filtered = signalProcessor.applyFIRFilter(dcRemoved, LOWPASS_FIR_COEFFS);

        // ── Step 5: statistical features ─────────────────────────────
        double[] stats = signalProcessor.computeStatistics(filtered);

        // ── Step 6: windowed FFT ──────────────────────────────────────
        double[] window = extractWindow(filtered, fftWindowSize);
        double[] complexSpectrum = signalProcessor.computeFFT(
            window, SignalProcessorNative.WINDOW_HANN);

        // ── Step 7: magnitude spectrum ────────────────────────────────
        double[] magnitudeSpectrum = signalProcessor.computeMagnitudeSpectrum(complexSpectrum);

        // ── Step 8: dominant frequency ────────────────────────────────
        double dominantFreqHz = signalProcessor.findDominantFrequency(
            magnitudeSpectrum, CANONICAL_SAMPLE_RATE_HZ);

        LOG.info(String.format(
            "Analysis complete — mean=%.4f rms=%.4f dominantFreq=%.2f Hz zcr=%.4f",
            stats[SignalProcessorNative.STATS_IDX_MEAN],
            stats[SignalProcessorNative.STATS_IDX_RMS],
            dominantFreqHz,
            stats[SignalProcessorNative.STATS_IDX_ZCR]));

        return new SensorAnalysisResult(
            stats, magnitudeSpectrum, complexSpectrum,
            dominantFreqHz, CANONICAL_SAMPLE_RATE_HZ, filtered.length);
    }

    /**
     * Performs only statistical analysis on a pre-acquired sample buffer.
     * Useful when callers already hold the raw signal and only need features.
     *
     * @param samples pre-acquired signal samples
     * @return        statistical features array (see {@link SignalProcessorNative#STATS_IDX_MEAN})
     */
    public double[] extractFeatures(double[] samples) {
        SignalProcessorNative.checkSignal(samples, "samples");
        return signalProcessor.computeStatistics(samples);
    }

    /**
     * Applies the standard filter chain (DC removal + low-pass) to an external
     * sample buffer without going through the full analysis pipeline.
     *
     * @param samples raw samples
     * @return        filtered samples
     */
    public double[] filter(double[] samples) {
        SignalProcessorNative.checkSignal(samples, "samples");
        double[] dcRemoved = signalProcessor.applyIIRFilter(samples, HIGHPASS_IIR_COEFFS);
        return signalProcessor.applyFIRFilter(dcRemoved, LOWPASS_FIR_COEFFS);
    }

    // ── accessors ─────────────────────────────────────────────────────

    /** Returns the input sample rate this analyser expects from the device. */
    public double getInputSampleRateHz() { return inputSampleRateHz; }

    /** Returns the FFT window size configured for this analyser. */
    public int getFftWindowSize() { return fftWindowSize; }

    // ── private helpers ───────────────────────────────────────────────

    /**
     * Reads {@code sampleCount} raw bytes from the device and converts each
     * byte to a normalised double in [-1.0, 1.0].
     */
    private double[] acquireRawSamples(int deviceHandle, int sampleCount) {
        byte[] rawBytes = new byte[sampleCount];
        int bytesRead = deviceController.readData(deviceHandle, rawBytes, sampleCount);
        if (bytesRead <= 0) {
            throw new SignalProcessingException(
                "Device read returned no data (handle=" + deviceHandle + ")",
                SignalProcessingException.Operation.UNKNOWN);
        }
        double[] samples = new double[bytesRead];
        for (int i = 0; i < bytesRead; i++) {
            // Normalise 8-bit signed raw value to [-1.0, 1.0]
            samples[i] = rawBytes[i] / 128.0;
        }
        return samples;
    }

    /**
     * Extracts a zero-padded window of exactly {@code windowSize} samples
     * from the centre of {@code signal}.
     */
    private static double[] extractWindow(double[] signal, int windowSize) {
        if (signal.length >= windowSize) {
            int offset = (signal.length - windowSize) / 2;
            return Arrays.copyOfRange(signal, offset, offset + windowSize);
        }
        // Zero-pad if signal is shorter than the window
        double[] padded = new double[windowSize];
        System.arraycopy(signal, 0, padded, 0, signal.length);
        return padded;
    }
}