public class NativeBinaryProcessor {

    static {
        System.loadLibrary("binaryproc");
    }

    /**
     * Inspects and transforms a binary payload.
     *
     * @param payload input binary data
     * @return transformed binary data
     */
    public static native byte[] process(byte[] payload);
}
