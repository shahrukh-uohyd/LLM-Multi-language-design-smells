public class NativeImageEncoder {

    static {
        System.loadLibrary("imageencoder");
    }

    /**
     * Encodes raw grayscale image data.
     *
     * @param pixels grayscale pixel data
     * @param width image width
     * @param height image height
     * @return encoded image data
     */
    public static native byte[] encode(
            byte[] pixels, int width, int height);
}
