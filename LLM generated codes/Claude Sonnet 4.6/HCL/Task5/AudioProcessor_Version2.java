package com.audio;

import java.util.Objects;

/**
 * JNI facade that exposes native multimedia audio-decoding features to Java.
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Construct an {@code AudioProcessor}.</li>
 *   <li>Call {@link #initialize(AudioFormat)} once.</li>
 *   <li>Call audio processing methods ({@link #decodeAudio}, etc.) as needed.</li>
 *   <li>Call {@link #shutdown()} (or use try-with-resources) to release resources.</li>
 * </ol>
 *
 * <h2>Native method naming</h2>
 * Every {@code native} method resolves to a C symbol following the standard
 * JNI naming convention:
 * <pre>
 *   Java_com_audio_AudioProcessor_&lt;methodName&gt;
 * </pre>
 *
 * <h2>Thread safety</h2>
 * All public methods are {@code synchronized} on the instance. Do not share
 * a single {@code AudioProcessor} across threads; create one per thread instead.
 */
public final class AudioProcessor implements AutoCloseable {

    // -------------------------------------------------------------------------
    // Static initialiser — loads the native library the first time this
    // class is referenced, before any instance or native method is used.
    // -------------------------------------------------------------------------
    static {
        NativeAudioLibraryLoader.load();
    }

    // -------------------------------------------------------------------------
    // Instance state
    // -------------------------------------------------------------------------

    /** Opaque native handle (pointer-sized integer) owned by the C layer. */
    private long          nativeHandle  = 0L;
    private AudioFormat   activeFormat  = null;
    private volatile boolean initialized = false;
    private volatile boolean shutdown    = false;

    // -------------------------------------------------------------------------
    // Public Java API
    // -------------------------------------------------------------------------

    /**
     * Initialises the native audio engine with the specified format.
     *
     * @param format desired PCM output format
     * @throws AudioException        if native initialisation fails
     * @throws IllegalStateException if already initialised or shut down
     * @throws NullPointerException  if {@code format} is {@code null}
     */
    public synchronized void initialize(AudioFormat format) {
        checkNotShutdown();
        if (initialized) {
            throw new IllegalStateException("AudioProcessor is already initialised.");
        }
        Objects.requireNonNull(format, "format must not be null.");

        nativeHandle = nativeInitialize(
                format.getSampleRate().getHz(),
                format.getBitDepth().getBits(),
                format.getChannels(),
                format.isBigEndian());

        if (nativeHandle == 0L) {
            throw new AudioException(
                "Native audio engine returned a null handle. "
              + "Verify that the native library was compiled correctly.");
        }

        activeFormat = format;
        initialized  = true;

        System.out.printf("[AudioProcessor] Initialized — format=%s  handle=0x%X%n",
                format, nativeHandle);
    }

    /**
     * Decodes a compressed audio stream into raw PCM samples.
     *
     * @param encodedAudio compressed audio bytes (e.g. MP3, AAC, OGG)
     * @return {@link AudioProcessingResult} with the decoded PCM and diagnostics
     * @throws AudioException           if decoding fails natively
     * @throws IllegalArgumentException if {@code encodedAudio} is empty
     * @throws IllegalStateException    if not initialised or shut down
     */
    public synchronized AudioProcessingResult decodeAudio(byte[] encodedAudio) {
        checkReady();
        Objects.requireNonNull(encodedAudio, "encodedAudio must not be null.");
        if (encodedAudio.length == 0) {
            throw new IllegalArgumentException("encodedAudio must not be empty.");
        }

        byte[] pcm = nativeDecodeAudio(nativeHandle, encodedAudio, encodedAudio.length);
        if (pcm == null || pcm.length == 0) {
            throw new AudioException("Native decoder returned empty PCM data.");
        }

        long  durationMs   = nativeGetDurationMs(nativeHandle);
        float peakAmp      = nativeGetPeakAmplitude(nativeHandle);
        float rmsLevel     = nativeGetRmsLevel(nativeHandle);
        boolean clipped    = nativeIsClipped(nativeHandle);

        return new AudioProcessingResult(pcm, activeFormat, durationMs,
                                          peakAmp, rmsLevel, clipped);
    }

    /**
     * Applies a gain (volume) adjustment to the supplied PCM buffer.
     *
     * @param pcmData  raw PCM audio bytes to modify (in-place semantics)
     * @param gainDb   gain in decibels; positive amplifies, negative attenuates
     * @return adjusted PCM audio bytes (may be a new buffer)
     * @throws AudioException        if the native operation fails
     * @throws IllegalStateException if not initialised or shut down
     */
    public synchronized byte[] applyGain(byte[] pcmData, float gainDb) {
        checkReady();
        Objects.requireNonNull(pcmData, "pcmData must not be null.");
        if (pcmData.length == 0) {
            throw new IllegalArgumentException("pcmData must not be empty.");
        }

        byte[] result = nativeApplyGain(nativeHandle, pcmData, pcmData.length, gainDb);
        if (result == null) {
            throw new AudioException("Native gain operation returned null.");
        }
        return result;
    }

    /**
     * Resamples PCM audio from the current format's sample rate to
     * {@code targetSampleRateHz}.
     *
     * @param pcmData           source PCM bytes
     * @param targetSampleRateHz desired output sample rate (e.g. 48000)
     * @return resampled PCM bytes
     * @throws AudioException if native resampling fails
     */
    public synchronized byte[] resample(byte[] pcmData, int targetSampleRateHz) {
        checkReady();
        Objects.requireNonNull(pcmData, "pcmData must not be null.");
        if (targetSampleRateHz <= 0) {
            throw new IllegalArgumentException(
                    "targetSampleRateHz must be > 0, got: " + targetSampleRateHz);
        }

        byte[] result = nativeResample(nativeHandle, pcmData, pcmData.length, targetSampleRateHz);
        if (result == null) {
            throw new AudioException("Native resampler returned null.");
        }
        return result;
    }

    /**
     * Returns the native audio library's version string.
     *
     * @throws IllegalStateException if not initialised or shut down
     */
    public synchronized String getLibraryVersion() {
        checkReady();
        return nativeGetVersion(nativeHandle);
    }

    /**
     * Returns the {@link AudioFormat} this processor was initialised with.
     *
     * @throws IllegalStateException if not initialised
     */
    public synchronized AudioFormat getActiveFormat() {
        if (!initialized) {
            throw new IllegalStateException("AudioProcessor is not initialised.");
        }
        return activeFormat;
    }

    /** Alias for {@link #shutdown()} — enables try-with-resources usage. */
    @Override
    public void close() {
        shutdown();
    }

    /**
     * Releases all native resources. Idempotent — safe to call more than once.
     */
    public synchronized void shutdown() {
        if (shutdown) return;
        if (initialized && nativeHandle != 0L) {
            nativeShutdown(nativeHandle);
            nativeHandle = 0L;
        }
        shutdown    = true;
        initialized = false;
        System.out.println("[AudioProcessor] Shutdown complete.");
    }

    // -------------------------------------------------------------------------
    // Native method declarations
    // All resolved by the JNI layer in audio_processor.cpp
    // -------------------------------------------------------------------------

    /** Initialises the native audio engine and returns an opaque handle. */
    private native long nativeInitialize(int sampleRateHz, int bitDepth,
                                          int channels,    boolean bigEndian);

    /** Decodes compressed audio bytes into raw PCM. */
    private native byte[] nativeDecodeAudio(long handle, byte[] encodedAudio, int length);

    /** Applies a dB gain adjustment to raw PCM data. */
    private native byte[] nativeApplyGain(long handle, byte[] pcmData, int length, float gainDb);

    /** Resamples PCM data to the target sample rate. */
    private native byte[] nativeResample(long handle, byte[] pcmData,
                                          int length, int targetSampleRateHz);

    /** Returns the duration (ms) of the most recently decoded audio. */
    private native long nativeGetDurationMs(long handle);

    /** Returns the peak amplitude [0.0, 1.0] of the most recently decoded audio. */
    private native float nativeGetPeakAmplitude(long handle);

    /** Returns the RMS level [0.0, 1.0] of the most recently decoded audio. */
    private native float nativeGetRmsLevel(long handle);

    /** Returns whether clipping was detected during the most recent decode. */
    private native boolean nativeIsClipped(long handle);

    /** Returns the native library version string. */
    private native String nativeGetVersion(long handle);

    /** Releases all resources held by the native engine for this handle. */
    private native void nativeShutdown(long handle);

    // -------------------------------------------------------------------------
    // Guards
    // -------------------------------------------------------------------------

    private void checkNotShutdown() {
        if (shutdown) {
            throw new IllegalStateException("AudioProcessor has been shut down.");
        }
    }

    private void checkReady() {
        checkNotShutdown();
        if (!initialized) {
            throw new IllegalStateException(
                    "AudioProcessor is not initialised. Call initialize(AudioFormat) first.");
        }
    }
}