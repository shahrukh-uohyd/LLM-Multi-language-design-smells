package com.imagefilter;

import java.util.Objects;

/**
 * Immutable result returned after a native filter operation.
 *
 * <p>Carries the output {@link ImageFrame}, the filter name, the
 * {@link FilterOptions} used, and wall-clock elapsed time.
 */
public final class FilterResult {

    private final String        filterName;
    private final ImageFrame    outputFrame;
    private final FilterOptions options;
    private final long          elapsedNanos;

    public FilterResult(String filterName, ImageFrame outputFrame,
                        FilterOptions options, long elapsedNanos) {
        this.filterName   = Objects.requireNonNull(filterName);
        this.outputFrame  = Objects.requireNonNull(outputFrame);
        this.options      = Objects.requireNonNull(options);
        this.elapsedNanos = elapsedNanos;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────
    public String        getFilterName()   { return filterName;   }
    public ImageFrame    getOutputFrame()  { return outputFrame;  }
    public FilterOptions getOptions()      { return options;      }
    public long          getElapsedNanos() { return elapsedNanos; }

    /**
     * Convenience: sample the centre pixel of the output frame for quick
     * visual verification in tests.
     */
    public int[] centreSampleRGBA() {
        int cx = outputFrame.getWidth()  / 2;
        int cy = outputFrame.getHeight() / 2;
        return outputFrame.getPixel(cx, cy);
    }

    @Override
    public String toString() {
        int[] c = centreSampleRGBA();
        return String.format(
            "FilterResult{ %-20s | %s | centre=RGBA(%3d,%3d,%3d,%3d) | %,d ns }",
            filterName, outputFrame, c[0], c[1], c[2], c[3], elapsedNanos);
    }
}