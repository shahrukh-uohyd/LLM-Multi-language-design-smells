public final class AudioProcessor {

    static {
        // Ensure native multimedia library is loaded first
        NativeAudioLibraryLoader.load();
    }

    /**
     * Decodes raw audio data using the native library.
     *
     * @param encodedAudio compressed audio bytes
     * @return decoded PCM audio data
     */
    public byte[] decode(byte[] encodedAudio) {
        if (encodedAudio == null) {
            throw new IllegalArgumentException("Audio data must not be null");
        }
        return nativeDecode(encodedAudio);
    }

    // Implemented in native code
    private native byte[] nativeDecode(byte[] encodedAudio);
}
