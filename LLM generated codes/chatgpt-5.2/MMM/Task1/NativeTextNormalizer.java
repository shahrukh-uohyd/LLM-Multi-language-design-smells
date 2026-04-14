public class NativeTextNormalizer {

    static {
        System.loadLibrary("textnormalize");
    }

    /**
     * Normalizes a single textual value.
     *
     * @param input text to normalize
     * @return normalized text
     */
    public static native String normalize(String input);
}
