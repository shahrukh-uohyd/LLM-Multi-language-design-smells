package com.example.multimedia.nativebridge;

import java.util.Map;

public final class AudioMetadataExtractor {

    static {
        System.loadLibrary("audio_metadata_native");
    }

    private AudioMetadataExtractor() {
        // utility class
    }

    /**
     * Extracts metadata from an audio stream or file.
     *
     * @param sourcePath path to an audio file or stream source
     * @return map of metadata keys to values (e.g. title, artist, duration)
     */
    public static native Map<String, String> extractMetadata(String sourcePath);
}
