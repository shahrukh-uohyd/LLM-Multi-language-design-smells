public class NativeImageFilter {

    static {
        NativeLoader.load();
    }

    /**
     * Applies a grayscale filter.
     *
     * @param pixels  ARGB pixels (length = width * height)
     * @param width   image width
     * @param height  image height
     * @return filtered ARGB pixels
     */
    public static native int[] grayscale(int[] pixels, int width, int height);
}
