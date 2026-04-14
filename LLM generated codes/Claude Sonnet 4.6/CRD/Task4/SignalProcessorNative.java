package com.app.signal;

import java.util.Objects;

/**
 * JNI entrypoint for native signal processing operations.
 *
 * <h2>Capabilities</h2>
 * <ul>
 *   <li><b>FFT / IFFT</b> — Fast Fourier Transform (Cooley–Tukey, radix-2)
 *       on power-of-two sample windows.</li>
 *   <li><b>FIR Filtering</b> — Finite Impulse Response filter with a
 *       caller-supplied coefficient kernel.</li>
 *   <li><b>IIR Filtering</b> — Biquad (second-order section) IIR filter.</li>
 *   <li><b>Resampling</b> — Linear-interpolation upsampling / downsampling.</li>
 *   <li><b>Statistics</b> — Mean, variance, RMS, peak, and zero-crossing rate
 *       computed natively for large signal windows.</li>
 *   <li><b>Convolution</b> — Direct linear convolution of two real sequences.</li>
 * </ul>
 *
 * <h2>Data conventions</h2>
 * <ul>
 *   <li>All signal samples are {@code double} (64-bit IEEE 754).</li>
 *   <li>Complex arrays are interleaved: {@code [re0, im0, re1, im1, ...]},
 *       so a size-N complex array has length {@code 2N}.</li>
 *   <li>Sample rate is expressed in Hz as a {@code double}.</li>
 * </ul>
 *
 * <p>Callers should prefer the higher-level {@link SensorAnalyzer} API.
 * This class is intentionally package-accessible for testability.</p>
 */
public class SignalProcessorNative {

    // ── window function type constants ────────────────────────────────

    /** Rectangular (no) window — flat spectrum, highest spectral leakage. */
    public static final int WINDOW_RECTANGULAR = 0;

    /** Hann window — good general-purpose spectral analysis. */
    public static final int WINDOW_HANN        = 1;

    /** Hamming window — slightly better side-lobe suppression than Hann. */
    public static final int WINDOW_HAMMING     = 2;

    /** Blackman window — excellent side-lobe suppression, wider main lobe. */
    public static final int WINDOW_BLACKMAN    = 3;

    // ── library loading ───────────────────────────────────────────────

    static {
        try {
            System.loadLibrary("signal_processor_native");
        } catch (UnsatisfiedLinkError e) {
            throw new ExceptionInInitializerError(
                "Failed to load native signal processing library: " + e.getMessage());
        }
    }

    // ── native declarations ───────────────────────────────────────────

    /**
     * Computes the real-input FFT of {@code samples}.
     *
     * <p>The length of {@code samples} must be a power of two.
     * An optional window function is applied before the transform.</p>
     *
     * @param samples      real-valued time-domain samples (length must be power of 2)
     * @param windowType   one of {@link #WINDOW_RECTANGULAR}, {@link #WINDOW_HANN},
     *                     {@link #WINDOW_HAMMING}, {@link #WINDOW_BLACKMAN}
     * @return             interleaved complex spectrum {@code [re0,im0, re1,im1, ...]}
     *                     of length {@code samples.length * 2}
     * @throws IllegalArgumentException  if {@code samples} is null/empty or length
     *                                   is not a power of two, or windowType is invalid
     * @throws SignalProcessingException if the native FFT operation fails
     */
    public native double[] computeFFT(double[] samples, int windowType);

    /**
     * Computes the inverse FFT (IFFT) of an interleaved complex spectrum,
     * returning real-valued time-domain samples.
     *
     * @param complexSpectrum interleaved complex array {@code [re0,im0,...]}
     *                        (length must be even and {@code length/2} a power of two)
     * @return                real-valued time-domain output of length
     *                        {@code complexSpectrum.length / 2}
     * @throws IllegalArgumentException  if input is null/empty or invalid length
     * @throws SignalProcessingException if the native IFFT operation fails
     */
    public native double[] computeIFFT(double[] complexSpectrum);

    /**
     * Applies a FIR (Finite Impulse Response) filter to {@code samples}.
     *
     * @param samples      input signal samples
     * @param coefficients FIR filter coefficients (the kernel / impulse response);
     *                     must not be null or empty
     * @return             filtered output signal (same length as {@code samples})
     * @throws IllegalArgumentException  if any argument is null or empty
     * @throws SignalProcessingException if the native filter operation fails
     */
    public native double[] applyFIRFilter(double[] samples, double[] coefficients);

    /**
     * Applies a biquad IIR (Infinite Impulse Response) filter.
     *
     * <p>The biquad section is defined by 5 coefficients
     * {@code [b0, b1, b2, a1, a2]} (Direct Form I, normalised so that a0=1).</p>
     *
     * @param samples    input signal samples
     * @param biquadCoeffs biquad coefficients {@code [b0, b1, b2, a1, a2]} — must be length 5
     * @return           filtered output signal (same length as {@code samples})
     * @throws IllegalArgumentException  if samples is null/empty or biquadCoeffs is not length 5
     * @throws SignalProcessingException if the native filter operation fails
     */
    public native double[] applyIIRFilter(double[] samples, double[] biquadCoeffs);

    /**
     * Resamples {@code samples} from {@code originalRate} to {@code targetRate}
     * using linear interpolation.
     *
     * @param samples      input signal samples
     * @param originalRate original sample rate in Hz (must be &gt; 0)
     * @param targetRate   desired output sample rate in Hz (must be &gt; 0)
     * @return             resampled signal at {@code targetRate}
     * @throws IllegalArgumentException  if samples is null/empty or rates are non-positive
     * @throws SignalProcessingException if the native resampling operation fails
     */
    public native double[] resample(double[] samples, double originalRate, double targetRate);

    /**
     * Computes descriptive statistics of the input signal natively.
     *
     * <p>Returned array layout (indices are named constants below):
     * {@code [mean, variance, stdDev, rms, min, max, peak, zeroCrossingRate]}</p>
     *
     * @param samples input signal samples — must not be null or empty
     * @return        statistics array of length {@value #STATS_ARRAY_LENGTH}
     * @throws IllegalArgumentException  if samples is null or empty
     * @throws SignalProcessingException if the native statistics operation fails
     * @see #STATS_IDX_MEAN
     */
    public native double[] computeStatistics(double[] samples);

    /**
     * Computes the linear convolution of {@code signal} with {@code kernel}.
     *
     * <p>Output length = {@code signal.length + kernel.length - 1}.</p>
     *
     * @param signal input signal
     * @param kernel convolution kernel (e.g. a filter impulse response)
     * @return       convolved output
     * @throws IllegalArgumentException  if either argument is null or empty
     * @throws SignalProcessingException if the native convolution fails
     */
    public native double[] convolve(double[] signal, double[] kernel);

    /**
     * Computes the magnitude spectrum (absolute values of FFT bins) from
     * an interleaved complex spectrum array.
     *
     * @param complexSpectrum interleaved complex FFT output from {@link #computeFFT}
     * @return                magnitude array of length {@code complexSpectrum.length / 2}
     * @throws IllegalArgumentException if complexSpectrum is null, empty, or odd-length
     * @throws SignalProcessingException if the native operation fails
     */
    public native double[] computeMagnitudeSpectrum(double[] complexSpectrum);

    /**
     * Finds the dominant frequency bin in a magnitude spectrum.
     *
     * @param magnitudeSpectrum output of {@link #computeMagnitudeSpectrum}
     * @param sampleRate        sample rate of the original signal in Hz
     * @return                  frequency in Hz of the highest-magnitude bin
     * @throws IllegalArgumentException  if magnitudeSpectrum is null/empty or
     *                                   sampleRate is non-positive
     * @throws SignalProcessingException if the native operation fails
     */
    public native double findDominantFrequency(double[] magnitudeSpectrum, double sampleRate);

    // ── statistics array index constants ─────────────────────────────

    /** Number of elements in the array returned by {@link #computeStatistics}. */
    public static final int STATS_ARRAY_LENGTH    = 8;

    public static final int STATS_IDX_MEAN        = 0;
    public static final int STATS_IDX_VARIANCE    = 1;
    public static final int STATS_IDX_STD_DEV     = 2;
    public static final int STATS_IDX_RMS         = 3;
    public static final int STATS_IDX_MIN         = 4;
    public static final int STATS_IDX_MAX         = 5;
    public static final int STATS_IDX_PEAK        = 6;
    public static final int STATS_IDX_ZCR         = 7;

    // ── Java-side validation helpers ──────────────────────────────────

    /** Validates a signal array: non-null and non-empty. */
    static void checkSignal(double[] signal, String paramName) {
        Objects.requireNonNull(signal, paramName + " must not be null");
        if (signal.length == 0) {
            throw new IllegalArgumentException(paramName + " must not be empty");
        }
    }

    /** Validates that a length is a positive power of two. */
    static void checkPowerOfTwo(int length, String paramName) {
        if (length <= 0 || (length & (length - 1)) != 0) {
            throw new IllegalArgumentException(
                paramName + " length must be a positive power of two, got: " + length);
        }
    }

    /** Validates a window type constant. */
    static void checkWindowType(int windowType) {
        if (windowType < WINDOW_RECTANGULAR || windowType > WINDOW_BLACKMAN) {
            throw new IllegalArgumentException(
                "windowType must be 0–3, got: " + windowType);
        }
    }

    /** Validates a sample rate is positive. */
    static void checkSampleRate(double rate, String paramName) {
        if (rate <= 0.0) {
            throw new IllegalArgumentException(paramName + " must be > 0, got: " + rate);
        }
    }
}