package com.multimedia.native_media;

/**
 * Provides native bindings for extracting metadata from a raw audio stream.
 *
 * <p>The native layer is expected to use a media-demuxing library
 * (e.g., FFmpeg {@code libavformat} / {@code libavcodec}) to open the
 * stream at the given URI, inspect its container and codec headers, and
 * populate the fields of an {@link AudioMetadata} instance.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 *   AudioMetadataExtractor extractor = new AudioMetadataExtractor();
 *   extractor.openAudioStream("/media/music/track.flac");
 *   AudioMetadata meta = extractor.extractMetadata("/media/music/track.flac");
 *   System.out.println(meta);
 * }</pre>
 *
 * <p><strong>Thread safety:</strong> A single instance is <em>not</em>
 * thread-safe.  Create one instance per thread, or synchronise externally.</p>
 */
public class AudioMetadataExtractor {

    static {
        // Loads libaudio_metadata.so (Linux/macOS) or audio_metadata.dll (Windows)
        System.loadLibrary("audio_metadata");
    }

    // ------------------------------------------------------------------
    // Native method declarations
    // ------------------------------------------------------------------

    /**
     * Opens and validates the audio stream at the given URI.
     *
     * <p>Must be called exactly once before {@link #extractMetadata(String)}.
     * Internally this allocates the native demuxer context, probes the
     * container format, and locates the primary audio stream within it.</p>
     *
     * @param streamUri absolute file path or network URI pointing to the
     *                  audio resource (e.g., {@code "/data/track.mp3"},
     *                  {@code "rtsp://192.168.1.10/live"}); must not be
     *                  {@code null} or empty
     * @throws IllegalArgumentException if {@code streamUri} is null or blank
     * @throws RuntimeException         if the stream cannot be opened,
     *                                  the format is unrecognised, or the
     *                                  native demuxer context allocation fails
     */
    public native void openAudioStream(String streamUri);

    /**
     * Extracts metadata from the previously opened audio stream.
     *
     * <p>Reads container-level tags (title, artist, album) and codec
     * parameters (sample rate, channel count, bit rate, codec name)
     * from the native demuxer context and packages them into a new
     * {@link AudioMetadata} object.</p>
     *
     * @param streamUri the same URI that was passed to
     *                  {@link #openAudioStream(String)}; used by the
     *                  native layer to identify the open context
     * @return a fully populated, non-null {@link AudioMetadata} instance
     * @throws IllegalStateException if {@link #openAudioStream(String)}
     *                               has not been called first
     * @throws RuntimeException      if metadata cannot be read from the
     *                               native demuxer context
     */
    public native AudioMetadata extractMetadata(String streamUri);

    // ------------------------------------------------------------------
    // Convenience wrappers (pure Java — no JNI overhead)
    // ------------------------------------------------------------------

    /**
     * Convenience method that opens the stream and extracts metadata in a
     * single call, combining {@link #openAudioStream} and
     * {@link #extractMetadata}.
     *
     * @param streamUri absolute file path or network URI
     * @return a fully populated {@link AudioMetadata} instance
     * @throws IllegalArgumentException if {@code streamUri} is null or blank
     */
    public AudioMetadata openAndExtract(String streamUri) {
        if (streamUri == null || streamUri.isBlank()) {
            throw new IllegalArgumentException("streamUri must not be null or blank.");
        }
        openAudioStream(streamUri);
        return extractMetadata(streamUri);
    }

    /**
     * Formats the duration stored in an {@link AudioMetadata} object into a
     * human-readable {@code HH:mm:ss} string without any native call.
     *
     * @param metadata a non-null {@link AudioMetadata} instance
     * @return formatted duration string
     */
    public String formatDuration(AudioMetadata metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("metadata must not be null.");
        }
        long totalSeconds = metadata.durationMs / 1000;
        long hours   = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}