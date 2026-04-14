/**
 * Java binding for the 'audio-codec-v2' native library.
 *
 * This class:
 *   1. Loads the native shared library (audio-codec-v2) when the class
 *      is first accessed via a static initialiser.
 *   2. Declares every native method that the library exposes.
 *   3. Provides pure-Java helpers for validation, result wrapping,
 *      and error reporting.
 *
 * Native library file names by platform:
 *   Linux   : libaudio-codec-v2.so
 *   macOS   : libaudio-codec-v2.dylib
 *   Windows : audio-codec-v2.dll
 *
 * Expected usage:
 *   AudioCodec codec = new AudioCodec();
 *   codec.openFile("track.prp");
 *   AudioCodec.DecodeResult result = codec.decodeNextFrame();
 *   System.out.println(result);
 *   codec.closeFile();
 */
public class AudioCodec {

    // ------------------------------------------------------------------ //
    //  Library load state                                                  //
    // ------------------------------------------------------------------ //

    /** True only when the native library loaded successfully. */
    private static final boolean LIBRARY_LOADED;

    /** Reason string when loading fails – null on success. */
    private static final String  LOAD_ERROR;

    // ------------------------------------------------------------------ //
    //  Static initialiser – runs once when the class is first accessed    //
    // ------------------------------------------------------------------ //

    static {
        boolean loaded = false;
        String  error  = null;

        try {
            /*
             * System.loadLibrary() searches directories listed in the
             * java.library.path system property.
             *
             * Supply the path at runtime, e.g.:
             *   java -Djava.library.path=/opt/audio-codec/lib \
             *        -cp out Main
             *
             * The JVM automatically prepends "lib" and appends the
             * platform extension (.so / .dylib / .dll), so the argument
             * here is just the bare library name.
             */
            System.loadLibrary("audio-codec-v2");
            loaded = true;
        } catch (UnsatisfiedLinkError ule) {
            error = "Failed to load native library 'audio-codec-v2': " +
                    ule.getMessage() +
                    " | Ensure the library is on java.library.path.";
            System.err.println("[AudioCodec] " + error);
        } catch (SecurityException se) {
            error = "Security manager denied loading 'audio-codec-v2': " +
                    se.getMessage();
            System.err.println("[AudioCodec] " + error);
        }

        LIBRARY_LOADED = loaded;
        LOAD_ERROR     = error;
    }

    // ------------------------------------------------------------------ //
    //  Constructor                                                         //
    // ------------------------------------------------------------------ //

    /**
     * Creates a new AudioCodec instance.
     *
     * @throws IllegalStateException if the native library could not be loaded
     */
    public AudioCodec() {
        requireLibrary();
    }

    // ------------------------------------------------------------------ //
    //  Native method declarations – Codec lifecycle                        //
    // ------------------------------------------------------------------ //

    /**
     * Opens a proprietary audio file and prepares the native decoder.
     * Must be called before any decode operation.
     *
     * @param filePath  absolute or relative path to the audio file
     * @return          a non-negative codec handle on success, -1 on failure
     */
    public native int openFile(String filePath);

    /**
     * Closes the currently open file and releases all native resources
     * associated with the decoder.
     *
     * @return  0 on success, a negative error code on failure
     */
    public native int closeFile();

    // ------------------------------------------------------------------ //
    //  Native method declarations – Decoding                              //
    // ------------------------------------------------------------------ //

    /**
     * Decodes the next available audio frame from the open file.
     *
     * @return  raw PCM samples as a {@code float[]} (interleaved channels),
     *          or {@code null} when the stream is exhausted or an error occurs
     */
    public native float[] decodeNextFrame();

    /**
     * Decodes a specific frame by its zero-based index.
     * Requires the format to support random access.
     *
     * @param frameIndex  zero-based index of the desired frame
     * @return            raw PCM samples, or {@code null} on error / EOF
     */
    public native float[] decodeFrameAt(int frameIndex);

    /**
     * Decodes all remaining frames into a single contiguous PCM buffer.
     * Use with caution on large files – prefer streaming via
     * {@link #decodeNextFrame()} for memory efficiency.
     *
     * @return  complete interleaved PCM float array, or {@code null} on error
     */
    public native float[] decodeAllFrames();

    // ------------------------------------------------------------------ //
    //  Native method declarations – Stream metadata                        //
    // ------------------------------------------------------------------ //

    /**
     * Returns the sample rate of the open audio stream.
     *
     * @return  sample rate in Hz (e.g. 44100, 48000), or -1 if unavailable
     */
    public native int getSampleRate();

    /**
     * Returns the number of audio channels in the open stream.
     *
     * @return  channel count (1 = mono, 2 = stereo, etc.), or -1 if unavailable
     */
    public native int getChannelCount();

    /**
     * Returns the bit depth of the encoded audio.
     *
     * @return  bits per sample (e.g. 16, 24, 32), or -1 if unavailable
     */
    public native int getBitDepth();

    /**
     * Returns the total number of decodable frames in the file.
     *
     * @return  frame count, or -1 if the format does not expose this value
     */
    public native int getTotalFrameCount();

    /**
     * Returns the total playback duration in milliseconds.
     *
     * @return  duration in ms, or -1 if undetermined
     */
    public native long getDurationMs();

    /**
     * Returns the encoded bitrate of the stream.
     *
     * @return  bitrate in kilobits per second (kbps), or -1 if unavailable
     */
    public native int getBitrateKbps();

    /**
     * Returns the proprietary format identifier string reported by the codec
     * (e.g. "PRP/2.1", "XAUD/4").
     *
     * @return  format string, or {@code null} if the file is not yet open
     */
    public native String getFormatIdentifier();

    // ------------------------------------------------------------------ //
    //  Native method declarations – Seek & control                         //
    // ------------------------------------------------------------------ //

    /**
     * Seeks the decode cursor to the frame nearest to the given timestamp.
     *
     * @param timestampMs  target position in milliseconds from the start
     * @return             actual position seeked to (ms), or -1 on failure
     */
    public native long seekToMs(long timestampMs);

    /**
     * Seeks directly to a frame by index.
     *
     * @param frameIndex  zero-based target frame
     * @return            0 on success, negative error code on failure
     */
    public native int seekToFrame(int frameIndex);

    /**
     * Resets the decode cursor to the beginning of the file.
     *
     * @return  0 on success, negative error code on failure
     */
    public native int reset();

    // ------------------------------------------------------------------ //
    //  Native method declarations – Post-decode processing                 //
    // ------------------------------------------------------------------ //

    /**
     * Applies the codec's built-in volume scaling to a PCM frame buffer.
     *
     * @param pcmData  PCM float samples to scale (modified in place)
     * @param gain     linear gain factor (1.0 = unity, 0.5 = -6 dB)
     * @return         0 on success, negative error code on failure
     */
    public native int applyGain(float[] pcmData, float gain);

    /**
     * Converts a PCM float buffer to interleaved 16-bit signed integers.
     * Useful for feeding downstream audio sinks that expect integer PCM.
     *
     * @param pcmData  source float samples in the range [-1.0, 1.0]
     * @return         16-bit PCM as a {@code byte[]} (little-endian),
     *                 or {@code null} on error
     */
    public native byte[] convertToInt16PCM(float[] pcmData);

    // ------------------------------------------------------------------ //
    //  Native method declarations – Library introspection                  //
    // ------------------------------------------------------------------ //

    /**
     * Returns the version string of the loaded native library.
     *
     * @return  version string (e.g. "audio-codec-v2  build 204  2026-01")
     */
    public native String getLibraryVersion();

    /**
     * Returns the list of proprietary file extensions this build of the
     * codec can decode (e.g. ["prp", "xaud", "paudio"]).
     *
     * @return  array of lowercase extension strings
     */
    public native String[] getSupportedExtensions();

    /**
     * Returns whether the native codec was compiled with hardware
     * acceleration support (DSP / SIMD offload).
     *
     * @return  {@code true} if hardware acceleration is available
     */
    public native boolean isHardwareAccelerated();

    // ------------------------------------------------------------------ //
    //  Pure-Java convenience API                                           //
    // ------------------------------------------------------------------ //

    /**
     * Opens a file and immediately collects its stream metadata into a
     * {@link StreamInfo} snapshot before any decoding begins.
     *
     * @param filePath  path to the audio file
     * @return          {@link StreamInfo} populated from the native codec
     * @throws AudioCodecException if the file cannot be opened
     */
    public StreamInfo openAndInspect(String filePath) {
        requireLibrary();
        if (filePath == null || filePath.isEmpty())
            throw new IllegalArgumentException("filePath must not be null or empty");

        int handle = openFile(filePath);
        if (handle < 0)
            throw new AudioCodecException(
                "Native codec could not open file: " + filePath +
                " (error code " + handle + ")");

        return new StreamInfo(
            filePath,
            getSampleRate(),
            getChannelCount(),
            getBitDepth(),
            getTotalFrameCount(),
            getDurationMs(),
            getBitrateKbps(),
            getFormatIdentifier()
        );
    }

    /**
     * Decodes the next frame and wraps it in a {@link DecodeResult},
     * including computed statistics.
     *
     * @return  {@link DecodeResult}, or {@code null} at end-of-stream
     * @throws AudioCodecException on a native decode error
     */
    public DecodeResult decodeNextFrameWrapped() {
        requireLibrary();
        float[] pcm = decodeNextFrame();
        if (pcm == null) return null;               // EOF
        if (pcm.length == 0)
            throw new AudioCodecException("Native decoder returned an empty frame");
        return new DecodeResult(pcm, getChannelCount(), getSampleRate());
    }

    /**
     * Returns a human-readable summary of the loaded library.
     *
     * @return  multiline information string
     */
    public String libraryInfo() {
        requireLibrary();
        String   version    = getLibraryVersion();
        String[] extensions = getSupportedExtensions();
        boolean  hwAccel    = isHardwareAccelerated();

        StringBuilder sb = new StringBuilder();
        sb.append("audio-codec-v2 native library\n");
        sb.append("  Version            : ").append(version).append('\n');
        sb.append("  Hardware accel     : ").append(hwAccel ? "yes" : "no").append('\n');
        sb.append("  Supported formats  : ");
        if (extensions == null || extensions.length == 0) {
            sb.append("(none reported)");
        } else {
            for (int i = 0; i < extensions.length; i++) {
                if (i > 0) sb.append(", ");
                sb.append('.').append(extensions[i]);
            }
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ //
    //  Library-state helpers                                               //
    // ------------------------------------------------------------------ //

    /**
     * Returns {@code true} if the native library loaded successfully.
     *
     * @return  load status
     */
    public static boolean isLibraryLoaded() {
        return LIBRARY_LOADED;
    }

    /**
     * Returns the error message from the failed library load attempt,
     * or {@code null} if loading succeeded.
     *
     * @return  load error string, or null
     */
    public static String getLoadError() {
        return LOAD_ERROR;
    }

    /**
     * Throws {@link IllegalStateException} when the library is not loaded.
     * Called at the top of every public method that delegates to native code.
     */
    private static void requireLibrary() {
        if (!LIBRARY_LOADED)
            throw new IllegalStateException(
                "Native library 'audio-codec-v2' is not available. " +
                "Cause: " + LOAD_ERROR);
    }

    // ------------------------------------------------------------------ //
    //  Value objects                                                       //
    // ------------------------------------------------------------------ //

    /**
     * Immutable snapshot of audio-stream metadata, captured immediately
     * after a file is opened.
     */
    public static class StreamInfo {

        public final String filePath;
        public final int    sampleRate;
        public final int    channelCount;
        public final int    bitDepth;
        public final int    totalFrames;
        public final long   durationMs;
        public final int    bitrateKbps;
        public final String formatIdentifier;

        public StreamInfo(String filePath, int sampleRate, int channelCount,
                          int bitDepth, int totalFrames, long durationMs,
                          int bitrateKbps, String formatIdentifier) {
            this.filePath         = filePath;
            this.sampleRate       = sampleRate;
            this.channelCount     = channelCount;
            this.bitDepth         = bitDepth;
            this.totalFrames      = totalFrames;
            this.durationMs       = durationMs;
            this.bitrateKbps      = bitrateKbps;
            this.formatIdentifier = formatIdentifier;
        }

        @Override
        public String toString() {
            return String.format(
                "StreamInfo{file='%s', format=%s, sampleRate=%d Hz, " +
                "channels=%d, bitDepth=%d, frames=%d, duration=%d ms, bitrate=%d kbps}",
                filePath, formatIdentifier, sampleRate,
                channelCount, bitDepth, totalFrames, durationMs, bitrateKbps);
        }
    }

    /**
     * Holds a single decoded PCM frame together with derived statistics.
     */
    public static class DecodeResult {

        public final float[] pcmSamples;    // interleaved float PCM
        public final int     channelCount;
        public final int     sampleRate;
        public final int     frameSize;     // total samples in this frame
        public final double  peakAmplitude; // max absolute sample value
        public final double  rmsAmplitude;  // root-mean-square amplitude

        public DecodeResult(float[] pcmSamples, int channelCount, int sampleRate) {
            this.pcmSamples   = pcmSamples;
            this.channelCount = channelCount;
            this.sampleRate   = sampleRate;
            this.frameSize    = pcmSamples.length;

            // Compute peak and RMS in one pass
            double peak = 0.0, sumSq = 0.0;
            for (float s : pcmSamples) {
                double abs = Math.abs(s);
                if (abs > peak) peak = abs;
                sumSq += (double) s * s;
            }
            this.peakAmplitude = peak;
            this.rmsAmplitude  = Math.sqrt(sumSq / pcmSamples.length);
        }

        /** Duration of this frame in milliseconds. */
        public double frameDurationMs() {
            if (sampleRate <= 0 || channelCount <= 0) return -1.0;
            return (frameSize / (double) channelCount) / sampleRate * 1000.0;
        }

        @Override
        public String toString() {
            return String.format(
                "DecodeResult{samples=%d, channels=%d, sampleRate=%d Hz, " +
                "durationMs=%.2f, peak=%.4f, rms=%.4f}",
                frameSize, channelCount, sampleRate,
                frameDurationMs(), peakAmplitude, rmsAmplitude);
        }
    }

    // ------------------------------------------------------------------ //
    //  Exception type                                                      //
    // ------------------------------------------------------------------ //

    /**
     * Thrown when a native codec operation fails.
     */
    public static class AudioCodecException extends RuntimeException {

        public AudioCodecException(String message) {
            super(message);
        }

        public AudioCodecException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}