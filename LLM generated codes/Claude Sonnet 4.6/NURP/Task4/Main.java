package com.imagefilter;

import java.util.List;

/**
 * Demonstrates all ten native image filters on a synthetic gradient frame.
 *
 * <p>Because the demo is self-contained (no file I/O required), it works
 * out of the box without any sample images. Each filter is applied to the
 * same input frame, and the centre-pixel RGBA sample is printed to let you
 * visually verify that the native code is transforming the data correctly.
 */
public final class Main {

    /** Demo image dimensions */
    private static final int WIDTH  = 64;
    private static final int HEIGHT = 64;

    public static void main(String[] args) {

        banner();

        // ── Synthetic input image ─────────────────────────────────────────────
        ImageFrame input = ImageFrame.syntheticGradient(WIDTH, HEIGHT);
        System.out.printf("[Main] Input: %s%n%n", input);

        // Print the centre pixel of the input so we have a reference point
        int[] cp = input.getPixel(WIDTH / 2, HEIGHT / 2);
        System.out.printf("[Main] Input centre pixel RGBA = (%d, %d, %d, %d)%n%n",
                          cp[0], cp[1], cp[2], cp[3]);

        // ── Build a FilterOptions configuration ───────────────────────────────
        FilterOptions opts = new FilterOptions.Builder()
                .brightnessDelta(40)
                .contrastFactor(1.6)
                .blurRadius(3)
                .blurSigma(1.2)
                .sharpenStrength(1.0)
                .edgeThreshold(40)
                .pixelateBlockSize(8)
                .build();

        // ── Run all filters ───────────────────────────────────────────────────
        try (ImageFilterEngine engine = new ImageFilterEngine()) {

            List<FilterResult> results = List.of(
                engine.applyGrayscale    (input, opts),
                engine.applyInvert       (input, opts),
                engine.applyBrightness   (input, opts),
                engine.applyContrast     (input, opts),
                engine.applyGaussianBlur (input, opts),
                engine.applySharpen      (input, opts),
                engine.applyEdgeDetection(input, opts),
                engine.applySepia        (input, opts),
                engine.applyPixelate     (input, opts),
                engine.applyEmboss       (input, opts)
            );

            // ── Print results ────────────────────────────────────────���────────
            System.out.println("┌─────────────────────────────────────────────────────────────────────────────┐");
            System.out.println("│                         Filter Results Summary                              │");
            System.out.println("├──────────────────────┬───────────────────────────────────────┬─────────────┤");
            System.out.printf ("│ %-20s │ %-37s │ %11s │%n", "Filter", "Centre Pixel RGBA", "Time (ns)");
            System.out.println("├──────────────────────┼───────────────────────────────────────┼─────────────┤");

            for (FilterResult r : results) {
                int[] c = r.centreSampleRGBA();
                System.out.printf("│ %-20s │ RGBA(%3d, %3d, %3d, %3d)             │ %,11d │%n",
                        r.getFilterName(), c[0], c[1], c[2], c[3], r.getElapsedNanos());
            }

            System.out.println("└──────────────────────┴───────────────────────────────────────┴─────────────┘");

            // ── Spot-check verifications ────��─────────────────────────────────
            System.out.println();
            System.out.println("── Spot-check Assertions ─────────────────────────────────────────────────────");
            verifyGrayscale (results.get(0));
            verifyInvert    (results.get(1), cp);
            verifyBrightness(results.get(2), cp, opts.getBrightnessDelta());
            verifySepia     (results.get(7));
        }

        System.out.println();
        System.out.println("[Main] All filters applied successfully.");
    }

    // -------------------------------------------------------------------------
    // Spot-check verifications
    // -------------------------------------------------------------------------

    /** In a grayscale image R == G == B for every pixel. */
    private static void verifyGrayscale(FilterResult r) {
        int[] c = r.centreSampleRGBA();
        boolean ok = (c[0] == c[1]) && (c[1] == c[2]);
        System.out.printf("  Grayscale: R==G==B at centre? %s  (R=%d G=%d B=%d)%n",
                          ok ? "PASS ✓" : "FAIL ✗", c[0], c[1], c[2]);
    }

    /** Inverted channel = 255 - original channel (ignoring alpha). */
    private static void verifyInvert(FilterResult r, int[] original) {
        int[] c = r.centreSampleRGBA();
        boolean ok = (c[0] == 255 - original[0]) &&
                     (c[1] == 255 - original[1]) &&
                     (c[2] == 255 - original[2]);
        System.out.printf("  Invert:    255-original at centre? %s  (got R=%d, expected=%d)%n",
                          ok ? "PASS ✓" : "FAIL ✗", c[0], 255 - original[0]);
    }

    /** Brightened channel must be >= original (for positive delta), clamped at 255. */
    private static void verifyBrightness(FilterResult r, int[] original, int delta) {
        int[] c  = r.centreSampleRGBA();
        int expect = Math.min(255, original[0] + delta);
        boolean ok = (c[0] == expect);
        System.out.printf("  Brightness:clamped shift at centre? %s  (got R=%d, expected=%d)%n",
                          ok ? "PASS ✓" : "FAIL ✗", c[0], expect);
    }

    /** After sepia, Blue channel is always the smallest of the three. */
    private static void verifySepia(FilterResult r) {
        int[] c  = r.centreSampleRGBA();
        boolean ok = (c[2] <= c[1]) && (c[1] <= c[0]);
        System.out.printf("  Sepia:     R >= G >= B at centre? %s  (R=%d G=%d B=%d)%n",
                          ok ? "PASS ✓" : "FAIL ✗", c[0], c[1], c[2]);
    }

    // -------------------------------------------------------------------------
    // Banner
    // -------------------------------------------------------------------------

    private static void banner() {
        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║        JNI Image Filter Engine — Demonstration              ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
        System.out.println();
    }
}