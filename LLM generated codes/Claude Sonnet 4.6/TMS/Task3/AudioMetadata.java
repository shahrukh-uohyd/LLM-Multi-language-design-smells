package com.multimedia.native_media;

/**
 * Immutable data carrier holding metadata extracted from a native audio stream.
 *
 * <p>Instances of this class are created by the Java layer and populated
 * by {@link AudioMetadataExtractor#extractMetadata(String)} after the
 * native layer fills the individual fields via JNI.</p>
 *
 * <p>All fields use boxed types so that {@code null} can represent
 * "not present in the stream" without an ambiguous sentinel value.</p>
 */
public final class AudioMetadata {

    /** Track title tag, or {@code null} if not embedded in the stream. */
    public final String  title;

    /** Artist / performer tag, or {@code null} if not present. */
    public final String  artist;

    /** Album tag, or {@code null} if not present. */
    public final String  album;

    /** Duration of the audio stream in milliseconds. */
    public final long    durationMs;

    /** Sample rate in Hz (e.g., 44100, 48000). */
    public final int     sampleRateHz;

    /** Number of audio channels (1 = mono, 2 = stereo, etc.). */
    public final int     channelCount;

    /** Audio codec name as reported by the native demuxer (e.g., "AAC", "MP3", "FLAC"). */
    public final String  codecName;

    /** Average bit rate in bits-per-second, or {@code -1} if unavailable. */
    public final int     bitRateBps;

    /**
     * Constructs an {@code AudioMetadata} instance.
     * Intended to be called from {@link AudioMetadataExtractor} after the
     * native layer has populated all fields.
     *
     * @param title        track title tag
     * @param artist       artist tag
     * @param album        album tag
     * @param durationMs   total duration in milliseconds
     * @param sampleRateHz sample rate in Hz
     * @param channelCount number of audio channels
     * @param codecName    codec identifier string
     * @param bitRateBps   average bit rate in bps, or {@code -1}
     */
    public AudioMetadata(String title,
                         String artist,
                         String album,
                         long   durationMs,
                         int    sampleRateHz,
                         int    channelCount,
                         String codecName,
                         int    bitRateBps) {
        this.title        = title;
        this.artist       = artist;
        this.album        = album;
        this.durationMs   = durationMs;
        this.sampleRateHz = sampleRateHz;
        this.channelCount = channelCount;
        this.codecName    = codecName;
        this.bitRateBps   = bitRateBps;
    }

    @Override
    public String toString() {
        return String.format(
            "AudioMetadata{title='%s', artist='%s', album='%s', " +
            "durationMs=%d, sampleRateHz=%d, channelCount=%d, " +
            "codecName='%s', bitRateBps=%d}",
            title, artist, album,
            durationMs, sampleRateHz, channelCount,
            codecName, bitRateBps);
    }
}