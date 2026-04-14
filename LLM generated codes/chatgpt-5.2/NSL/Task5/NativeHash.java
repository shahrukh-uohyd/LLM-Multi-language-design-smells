public class NativeHash {

    static {
        try {
            System.loadLibrary("nativehash");
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException(
                "Failed to load native library 'nativehash'", e);
        }
    }

    /**
     * Computes SHA-256 hash of input data.
     *
     * @param data input bytes
     * @return SHA-256 hash (32 bytes)
     */
    public static native byte[] sha256(byte[] data);
}
