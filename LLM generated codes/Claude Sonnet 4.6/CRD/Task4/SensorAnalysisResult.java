package com.app.sensor;

import com.app.signal.SignalProcessorNative;
import java.util.Arrays;

/**
 * Immutable container for the output of a {@link SensorAnalyzer#analyze} run.
 *
 * <p>All arrays are defensively copied on construction so that callers cannot
 * mutate internal state.</p>
 */
public final class SensorAnalysisResult {

    private final double[] statistics;
    private final double[] magnitudeSpectrum;
    private final double[] complexSpectrum;
    private final double   dominantFrequencyHz;
    private final double   sampleRateHz;
    private final int      sampleCount;

    SensorAnalysisResult(double[] statistics,
                         double[] magnitudeSpectrum,
                         double[] complexSpectrum,
                         double   dominantFrequencyHz,
                         double   sampleRateHz,
                         int      sampleCount) {
        this.statistics          = Arrays.copyOf(statistics, statistics.length);
        this.magnitudeSpectrum   = Arrays.copyOf(magnitudeSpectrum, magnitudeSpectrum.length);
        this.complexSpectrum     = Arrays.copyOf(complexSpectrum, complexSpectrum.length);
        this.dominantFrequencyHz = dominantFrequencyHz;
        this.sampleRateHz        = sampleRateHz;
        this.sampleCount         = sampleCount;
    }

    // ── stat convenience accessors ────────────────────────────────────

    public double getMean()            { return statistics[SignalProcessorNative.STATS_IDX_MEAN]; }
    public double getVariance()        { return statistics[SignalProcessorNative.STATS_IDX_VARIANCE]; }
    public double getStdDev()          { return statistics[SignalProcessorNative.STATS_IDX_STD_DEV]; }
    public double getRms()             { return statistics[SignalProcessorNative.STATS_IDX_RMS]; }
    public double getMin()             { return statistics[SignalProcessorNative.STATS_IDX_MIN]; }
    public double getMax()             { return statistics[SignalProcessorNative.STATS_IDX_MAX]; }
    public double getPeak()            { return statistics[SignalProcessorNative.STATS_IDX_PEAK]; }
    public double getZeroCrossingRate(){ return statistics[SignalProcessorNative.STATS_IDX_ZCR]; }

    // ── spectral accessors ────────────────────────────────────────────

    public double   getDominantFrequencyHz()  { return dominantFrequencyHz; }
    public double[] getMagnitudeSpectrum()    { return Arrays.copyOf(magnitudeSpectrum, magnitudeSpectrum.length); }
    public double[] getComplexSpectrum()      { return Arrays.copyOf(complexSpectrum,   complexSpectrum.length); }
    public double   getSampleRateHz()         { return sampleRateHz; }
    public int      getSampleCount()          { return sampleCount; }

    /** Returns the frequency resolution of the spectrum in Hz/bin. */
    public double getFrequencyResolutionHz() {
        return sampleRateHz / magnitudeSpectrum.length;
    }

    @Override
    public String toString() {
        return String.format(
            "SensorAnalysisResult{samples=%d, rate=%.1fHz, mean=%.4f, rms=%.4f, "
          + "peak=%.4f, zcr=%.4f, dominantFreq=%.2fHz, spectrumBins=%d}",
            sampleCount, sampleRateHz, getMean(), getRms(),
            getPeak(), getZeroCrossingRate(), dominantFrequencyHz,
            magnitudeSpectrum.length);
    }
}