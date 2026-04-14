package com.imagefilter;

/**
 * Immutable configuration object passed to each native filter call.
 *
 * <p>Fields that are irrelevant to a specific filter are simply ignored
 * by the native code, so a single {@code FilterOptions} object can be
 * safely reused across different filter types.
 *
 * <h2>Supported Filters and Their Relevant Parameters</h2>
 * <table border="1" cellpadding="4">
 *   <tr><th>Filter</th><th>Used parameters</th></tr>
 *   <tr><td>Grayscale</td><td><i>none (uses ITU-R BT.601 luminance weights)</i></td></tr>
 *   <tr><td>Invert</td><td><i>none</i></td></tr>
 *   <tr><td>Brightness</td><td>{@code brightnessDelta}</td></tr>
 *   <tr><td>Contrast</td><td>{@code contrastFactor}</td></tr>
 *   <tr><td>Gaussian Blur</td><td>{@code blurRadius}, {@code blurSigma}</td></tr>
 *   <tr><td>Sharpen</td><td>{@code sharpenStrength}</td></tr>
 *   <tr><td>Edge Detection</td><td>{@code edgeThreshold}</td></tr>
 *   <tr><td>Sepia Tone</td><td><i>none (fixed classic sepia matrix)</i></td></tr>
 *   <tr><td>Pixelate</td><td>{@code pixelateBlockSize}</td></tr>
 *   <tr><td>Emboss</td><td><i>none (fixed 3×3 emboss kernel)</i></td></tr>
 * </table>
 */
public final class FilterOptions {

    // ── Brightness / Contrast ─────────────────────────────────────────────────
    /** Additive brightness shift: positive = brighter, negative = darker. Range [-255, 255]. */
    private final int    brightnessDelta;

    /** Multiplicative contrast factor. 1.0 = no change, >1.0 = more contrast. Range [0.0, 4.0]. */
    private final double contrastFactor;

    // ── Gaussian Blur ─────────────────────────────────────────────────────────
    /** Blur kernel half-radius in pixels. 0 = no blur. Typical values: 1–10. */
    private final int    blurRadius;

    /** Standard deviation of the Gaussian kernel. Ignored when blurRadius == 0. */
    private final double blurSigma;

    // ── Sharpen ───────────────────────────────────────────────────────────────
    /** Unsharp-mask blend strength. 0.0 = no sharpening, 1.0 = full sharpen. */
    private final double sharpenStrength;

    // ── Edge Detection ────────────────────────────────────────────────────────
    /** Sobel gradient magnitude threshold (0–255). Pixels above = edge (white). */
    private final int    edgeThreshold;

    // ── Pixelate ──────────────────────────────────────────────────────────────
    /** Side length of each mosaic block in pixels. Must be >= 1. */
    private final int    pixelateBlockSize;

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    private FilterOptions(Builder b) {
        this.brightnessDelta   = b.brightnessDelta;
        this.contrastFactor    = b.contrastFactor;
        this.blurRadius        = b.blurRadius;
        this.blurSigma         = b.blurSigma;
        this.sharpenStrength   = b.sharpenStrength;
        this.edgeThreshold     = b.edgeThreshold;
        this.pixelateBlockSize = b.pixelateBlockSize;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────
    public int    getBrightnessDelta()   { return brightnessDelta;   }
    public double getContrastFactor()    { return contrastFactor;    }
    public int    getBlurRadius()        { return blurRadius;        }
    public double getBlurSigma()         { return blurSigma;         }
    public double getSharpenStrength()   { return sharpenStrength;   }
    public int    getEdgeThreshold()     { return edgeThreshold;     }
    public int    getPixelateBlockSize() { return pixelateBlockSize; }

    // ── Convenience factory — sensible defaults ────────────────────────────────
    public static FilterOptions defaults() { return new Builder().build(); }

    @Override
    public String toString() {
        return String.format(
            "FilterOptions{brightness=%d, contrast=%.2f, blurRadius=%d, " +
            "blurSigma=%.2f, sharpen=%.2f, edgeThreshold=%d, pixelate=%d}",
            brightnessDelta, contrastFactor, blurRadius, blurSigma,
            sharpenStrength, edgeThreshold, pixelateBlockSize);
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static final class Builder {
        private int    brightnessDelta   =  30;
        private double contrastFactor    =  1.5;
        private int    blurRadius        =   2;
        private double blurSigma         =  1.0;
        private double sharpenStrength   =  1.0;
        private int    edgeThreshold     =  50;
        private int    pixelateBlockSize =  10;

        public Builder brightnessDelta(int v)   { brightnessDelta   = v; return this; }
        public Builder contrastFactor(double v) { contrastFactor    = v; return this; }
        public Builder blurRadius(int v)        { blurRadius        = v; return this; }
        public Builder blurSigma(double v)      { blurSigma         = v; return this; }
        public Builder sharpenStrength(double v){ sharpenStrength   = v; return this; }
        public Builder edgeThreshold(int v)     { edgeThreshold     = v; return this; }
        public Builder pixelateBlockSize(int v) { pixelateBlockSize = v; return this; }

        public FilterOptions build() { return new FilterOptions(this); }
    }
}