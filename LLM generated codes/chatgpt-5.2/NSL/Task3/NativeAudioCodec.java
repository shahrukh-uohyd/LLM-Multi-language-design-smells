public class NativeAudioCodec {

    // Static initializer: runs once when the class is first loaded
    static {
        try {
            System.loadLibrary("audio-codec-v2");
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException(
                "Failed to load native library 'audio-codec-v2'", e);
        }
    }

    /**
     * Decodes a proprietary audio file.
     *
     * @param encodedData raw encoded audio bytes
     * @return decoded PCM audio data
     */
    public static native byte[] decode(byte[] encodedData);

    /**
     * Retrieves basic information about the audio stream.
     *
     * @param encodedData raw encoded audio bytes
     * @return int[] containing metadata such as
     *         {sampleRate, channels, bitDepth}
     */
    public static native int[] getAudioInfo(byte[] encodedData);
}
