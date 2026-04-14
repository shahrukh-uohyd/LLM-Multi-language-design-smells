package com.imagefilter;

import java.util.Objects;

/**
 * Immutable container for raw RGBA image data exchanged between
 * Java and the native filtering layer.
 *
 * <h2>Pixel Layout</h2>
 * <p>Pixels are stored as a flat {@code byte[]} in <em>row-major</em> order.
 * Each pixel occupies exactly <b>4 bytes</b> in RGBA channel order:
 * <pre>
 *   index = (row * width + col) * 4
 *   byte[index + 0]  →  Red   (0-255)
 *   byte[index + 1]  →  Green (0-255)
 *   byte[index + 2]  →  Blue  (0-255)
 *   byte[index + 3]  →  Alpha (0-255)
 * </pre>
 */
public final class ImageFrame {

    /** Bytes per pixel: R, G, B, A. */
    public static final int CHANNELS = 4;

    private final int    width;
    private final int    height;
    private final byte[] pixels;   // always a defensive copy

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Creates an {@code ImageFrame} from a defensive copy of {@code pixels}.
     *
     * @param width  image width in pixels (must be &gt; 0)
     * @param height image height in pixels (must be &gt; 0)
     * @param pixels raw RGBA bytes; length must equal {@code width * height * 4}
     */
    public ImageFrame(int width, int height, byte[] pixels) {
        if (width  <= 0) throw new IllegalArgumentException("width must be > 0");
        if (height <= 0) throw new IllegalArgumentException("height must be > 0");
        Objects.requireNonNull(pixels, "pixels must not be null");

        int expected = width * height * CHANNELS;
        if (pixels.length != expected)
            throw new IllegalArgumentException(
                    "pixels.length=" + pixels.length +
                    " but width*height*4=" + expected);

        this.width  = width;
        this.height = height;
        this.pixels = pixels.clone();   // defensive copy — frame is immutable
    }

    // -------------------------------------------------------------------------
    // Factory helpers
    // -------------------------------------------------------------------------

    /**
     * Creates a solid-colour frame filled with the given RGBA value.
     * Useful for unit tests and synthetic benchmarks.
     */
    public static ImageFrame solidColor(int width, int height,
                                        int r, int g, int b, int a) {
        byte[] px = new byte[width * height * CHANNELS];
        for (int i = 0; i < width * height; i++) {
            px[i * CHANNELS]     = (byte) r;
            px[i * CHANNELS + 1] = (byte) g;
            px[i * CHANNELS + 2] = (byte) b;
            px[i * CHANNELS + 3] = (byte) a;
        }
        return new ImageFrame(width, height, px);
    }

    /**
     * Creates a synthetic gradient frame (horizontal red → blue) useful
     * for visually verifying filter output.
     */
    public static ImageFrame syntheticGradient(int width, int height) {
        byte[] px = new byte[width * height * CHANNELS];
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int idx = (row * width + col) * CHANNELS;
                px[idx]     = (byte)(255 * col / Math.max(width  - 1, 1)); // R
                px[idx + 1] = (byte)(255 * row / Math.max(height - 1, 1)); // G
                px[idx + 2] = (byte)(255 - 255 * col / Math.max(width - 1, 1)); // B
                px[idx + 3] = (byte) 255;                                   // A
            }
        }
        return new ImageFrame(width, height, px);
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public int    getWidth()  { return width;  }
    public int    getHeight() { return height; }

    /** Returns a defensive copy of the raw RGBA pixel data. */
    public byte[] getPixels() { return pixels.clone(); }

    /**
     * Returns the pixel at {@code (col, row)} as an {@code int[4]} array
     * in RGBA order.
     */
    public int[] getPixel(int col, int row) {
        int idx = (row * width + col) * CHANNELS;
        return new int[]{
            pixels[idx]     & 0xFF,
            pixels[idx + 1] & 0xFF,
            pixels[idx + 2] & 0xFF,
            pixels[idx + 3] & 0xFF
        };
    }

    /** Total number of bytes in the pixel buffer. */
    public int byteCount() { return pixels.length; }

    // -------------------------------------------------------------------------
    // Display
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return String.format("ImageFrame[%d×%d, %d bytes RGBA]",
                             width, height, pixels.length);
    }
}