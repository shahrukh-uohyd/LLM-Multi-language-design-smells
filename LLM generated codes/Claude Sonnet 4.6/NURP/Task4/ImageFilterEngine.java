package com.imagefilter;

/**
 * JNI bridge to the {@code image_filter} native library.
 *
 * <h2>Filters Available</h2>
 * <ul>
 *   <li><b>Grayscale</b>   – ITU-R BT.601 luminance conversion</li>
 *   <li><b>Invert</b>      – Bitwise complement of each RGB channel</li>
 *   <li><b>Brightness</b>  – Additive per-channel shift with clamping</li>
 *   <li><b>Contrast</b>    – Multiplicative factor around mid-grey (128)</li>
 *   <li><b>Gaussian Blur</b> – Separable Gaussian kernel, configurable radius & σ</li>
 *   <li><b>Sharpen</b>     – Unsharp mask (original – blurred) blend</li>
 *   <li><b>Edge Detection</b> – Sobel operator with configurable threshold</li>
 *   <li><b>Sepia</b>       – Classic photographic sepia tone matrix</li>
 *   <li><b>Pixelate</b>    – Mosaic/block average, configurable block size</li>
 *   <li><b>Emboss</b>      – Fixed 3×3 emboss convolution kernel</li>
 * </ul>
 *
 * <p>All {@code private native} methods receive and return raw RGBA
 * {@code byte[]} arrays together with width/height metadata.
 * The public wrapper methods build {@link ImageFrame} / {@link FilterResult}
 * objects, time the native call, and validate inputs before dispatch.
 */
public final class ImageFilterEngine implements AutoCloseable {

    static {
        NativeLibraryLoader.load();
    }

    // =========================================================================
    // Native method declarations  (implemented in image_filter.c)
    // =========================================================================

    /**
     * All native filter methods follow this contract:
     * <ul>
     *   <li>{@code pixels}  – flat RGBA byte array (length = width * height * 4)</li>
     *   <li>{@code width}   – frame width in pixels</li>
     *   <li>{@code height}  – frame height in pixels</li>
     *   <li>Extra params    – filter-specific numeric arguments</li>
     *   <li>Returns         – new RGBA byte array of identical dimensions</li>
     * </ul>
     */

    // ── Point filters (pixel-independent) ────────────────────────────────────
    private native byte[] nativeGrayscale(byte[] pixels, int width, int height);
    private native byte[] nativeInvert   (byte[] pixels, int width, int height);
    private native byte[] nativeBrightness(byte[] pixels, int width, int height,
                                           int delta);
    private native byte[] nativeContrast  (byte[] pixels, int width, int height,
                                           double factor);
    private native byte[] nativeSepia     (byte[] pixels, int width, int height);

    // ── Convolution-based filters (neighbourhood-dependent) ──────────────────
    private native byte[] nativeGaussianBlur(byte[] pixels, int width, int height,
                                             int radius, double sigma);
    private native byte[] nativeSharpen     (byte[] pixels, int width, int height,
                                             double strength);
    private native byte[] nativeEdgeDetect  (byte[] pixels, int width, int height,
                                             int threshold);
    private native byte[] nativeEmboss      (byte[] pixels, int width, int height);

    // ── Geometric / spatial filters ───────────────────────────────────────────
    private native byte[] nativePixelate(byte[] pixels, int width, int height,
                                         int blockSize);

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Converts the image to grayscale using ITU-R BT.601 luminance weights:
     * {@code Y = 0.299·R + 0.587·G + 0.114·B}.
     * Alpha channel is preserved unchanged.
     */
    public FilterResult applyGrayscale(ImageFrame frame, FilterOptions opts) {
        validate(frame);
        long t0  = System.nanoTime();
        byte[] out = nativeGrayscale(frame.getPixels(), frame.getWidth(), frame.getHeight());
        return result("Grayscale", frame, out, opts, t0);
    }

    /**
     * Inverts each RGB channel: {@code out = 255 - in}.
     * Alpha channel is preserved unchanged.
     */
    public FilterResult applyInvert(ImageFrame frame, FilterOptions opts) {
        validate(frame);
        long t0  = System.nanoTime();
        byte[] out = nativeInvert(frame.getPixels(), frame.getWidth(), frame.getHeight());
        return result("Invert", frame, out, opts, t0);
    }

    /**
     * Shifts all RGB channels by {@link FilterOptions#getBrightnessDelta()} and
     * clamps each channel to [0, 255]. Alpha is preserved.
     */
    public FilterResult applyBrightness(ImageFrame frame, FilterOptions opts) {
        validate(frame);
        long t0  = System.nanoTime();
        byte[] out = nativeBrightness(frame.getPixels(), frame.getWidth(), frame.getHeight(),
                                      opts.getBrightnessDelta());
        return result("Brightness", frame, out, opts, t0);
    }

    /**
     * Adjusts contrast by scaling each channel around mid-grey (128) by
     * {@link FilterOptions#getContrastFactor()}.
     * {@code out = clamp(factor * (in - 128) + 128)}.
     */
    public FilterResult applyContrast(ImageFrame frame, FilterOptions opts) {
        validate(frame);
        long t0  = System.nanoTime();
        byte[] out = nativeContrast(frame.getPixels(), frame.getWidth(), frame.getHeight(),
                                    opts.getContrastFactor());
        return result("Contrast", frame, out, opts, t0);
    }

    /**
     * Applies a separable Gaussian blur with the given {@code blurRadius} and
     * {@code blurSigma}. Uses two-pass (horizontal then vertical) convolution
     * for O(n·r) complexity instead of O(n·r²).
     */
    public FilterResult applyGaussianBlur(ImageFrame frame, FilterOptions opts) {
        validate(frame);
        if (opts.getBlurRadius() < 0)
            throw new IllegalArgumentException("blurRadius must be >= 0");
        long t0  = System.nanoTime();
        byte[] out = nativeGaussianBlur(frame.getPixels(), frame.getWidth(), frame.getHeight(),
                                        opts.getBlurRadius(), opts.getBlurSigma());
        return result("GaussianBlur", frame, out, opts, t0);
    }

    /**
     * Sharpens the image using an unsharp-mask technique:
     * {@code out = original + strength * (original - blurred)}.
     */
    public FilterResult applySharpen(ImageFrame frame, FilterOptions opts) {
        validate(frame);
        long t0  = System.nanoTime();
        byte[] out = nativeSharpen(frame.getPixels(), frame.getWidth(), frame.getHeight(),
                                   opts.getSharpenStrength());
        return result("Sharpen", frame, out, opts, t0);
    }

    /**
     * Detects edges using the Sobel operator. Pixels whose gradient magnitude
     * exceeds {@link FilterOptions#getEdgeThreshold()} are drawn white;
     * all others are drawn black.
     */
    public FilterResult applyEdgeDetection(ImageFrame frame, FilterOptions opts) {
        validate(frame);
        long t0  = System.nanoTime();
        byte[] out = nativeEdgeDetect(frame.getPixels(), frame.getWidth(), frame.getHeight(),
                                      opts.getEdgeThreshold());
        return result("EdgeDetection", frame, out, opts, t0);
    }

    /**
     * Applies a classic photographic sepia-tone colour matrix.
     * Alpha channel is preserved unchanged.
     */
    public FilterResult applySepia(ImageFrame frame, FilterOptions opts) {
        validate(frame);
        long t0  = System.nanoTime();
        byte[] out = nativeSepia(frame.getPixels(), frame.getWidth(), frame.getHeight());
        return result("Sepia", frame, out, opts, t0);
    }

    /**
     * Creates a mosaic effect by averaging pixel colours within each
     * {@code pixelateBlockSize × pixelateBlockSize} block.
     */
    public FilterResult applyPixelate(ImageFrame frame, FilterOptions opts) {
        validate(frame);
        if (opts.getPixelateBlockSize() < 1)
            throw new IllegalArgumentException("pixelateBlockSize must be >= 1");
        long t0  = System.nanoTime();
        byte[] out = nativePixelate(frame.getPixels(), frame.getWidth(), frame.getHeight(),
                                    opts.getPixelateBlockSize());
        return result("Pixelate", frame, out, opts, t0);
    }

    /**
     * Applies a fixed 3×3 emboss convolution kernel, producing a
     * raised-relief sculptural effect with a grey neutral point (128).
     */
    public FilterResult applyEmboss(ImageFrame frame, FilterOptions opts) {
        validate(frame);
        long t0  = System.nanoTime();
        byte[] out = nativeEmboss(frame.getPixels(), frame.getWidth(), frame.getHeight());
        return result("Emboss", frame, out, opts, t0);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void validate(ImageFrame frame) {
        if (frame == null) throw new IllegalArgumentException("frame must not be null");
    }

    private static FilterResult result(String name, ImageFrame src,
                                       byte[] rawOut, FilterOptions opts, long t0) {
        ImageFrame out = new ImageFrame(src.getWidth(), src.getHeight(), rawOut);
        return new FilterResult(name, out, opts, System.nanoTime() - t0);
    }

    /** No-op; present for try-with-resources compatibility. */
    @Override public void close() {}
}