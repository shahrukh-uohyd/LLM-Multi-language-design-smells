import java.util.Arrays;

/**
 * NumericalAnalyzer.java
 *
 * Java host class that:
 *  - Declares native numerical-analysis methods.
 *  - Builds a representative set of double[] and int[] datasets.
 *  - Passes each dataset to the native layer via JNI.
 *  - Receives, validates, and displays the results.
 *
 * Native operations per dataset
 * ─────────────────────────────
 *  ① aggregateStats   – min, max, sum, mean, variance, std-dev
 *  ② validateDataset  – detect NaN, ±Infinity, out-of-range values
 *  ③ normalizeDataset – min-max normalisation → [0.0, 1.0]
 *  ④ histogramDataset – frequency distribution across N equal-width bins
 *  ⑤ dotProduct       – dot product of two equal-length double arrays
 */
public class NumericalAnalyzer {

    // ---------------------------------------------------------------
    // Native method declarations
    // ---------------------------------------------------------------

    /**
     * Computes six aggregation statistics over a double array.
     *
     * @param data non-null double array (may be empty)
     * @return double[6] = { min, max, sum, mean, variance, stdDev }
     *         All elements are Double.NaN when data is empty.
     */
    public native double[] aggregateStats(double[] data);

    /**
     * Validates every element of the array.
     *
     * @param data    non-null double array to validate
     * @param lo      inclusive lower bound for range check
     * @param hi      inclusive upper bound for range check
     * @return int[4] = { nanCount, infCount, outOfRangeCount, validCount }
     */
    public native int[] validateDataset(double[] data, double lo, double hi);

    /**
     * Min-max normalises the array to [0.0, 1.0].
     * If min == max every element maps to 0.0.
     *
     * @param data non-null double array
     * @return new double[] with normalised values
     */
    public native double[] normalizeDataset(double[] data);

    /**
     * Builds a histogram of {@code data} into {@code bins} equal-width buckets
     * spanning [min(data), max(data)].
     *
     * @param data non-null, non-empty double array
     * @param bins number of histogram bins (>= 1)
     * @return int[] of length {@code bins} with per-bin counts
     */
    public native int[] histogramDataset(double[] data, int bins);

    /**
     * Computes the dot product of two equal-length double arrays.
     *
     * @param a first  vector (non-null, length == b.length)
     * @param b second vector (non-null, length == a.length)
     * @return scalar dot product
     */
    public native double dotProduct(double[] a, double[] b);

    // ---------------------------------------------------------------
    // Static initialiser — load the shared library
    // ---------------------------------------------------------------
    static {
        System.loadLibrary("numanalyzer");
    }

    // ---------------------------------------------------------------
    // Dataset factory methods
    // ---------------------------------------------------------------

    /** Linear ramp: 1.0, 2.0, … , 100.0 */
    private static double[] buildLinearRamp() {
        double[] d = new double[100];
        for (int i = 0; i < d.length; i++) d[i] = i + 1.0;
        return d;
    }

    /** Gaussian-ish samples centred on 50 (Box-Muller-free approximation) */
    private static double[] buildGaussianLike() {
        double[] d = new double[200];
        java.util.Random rng = new java.util.Random(42L);
        for (int i = 0; i < d.length; i++) {
            // sum of 12 uniforms ≈ N(6,1); shift to N(50,10)
            double u = 0;
            for (int k = 0; k < 12; k++) u += rng.nextDouble();
            d[i] = (u - 6.0) * 10.0 + 50.0;
        }
        return d;
    }

    /** Dataset containing NaN and Infinity sentinel values */
    private static double[] buildDirtyDataset() {
        return new double[]{
            1.0, Double.NaN, 3.0, Double.POSITIVE_INFINITY,
            -2.0, Double.NEGATIVE_INFINITY, 7.5, Double.NaN,
            100.0, -0.5
        };
    }

    /** Constant dataset — all values identical (edge-case for normalisation) */
    private static double[] buildConstantDataset() {
        double[] d = new double[10];
        Arrays.fill(d, 42.0);
        return d;
    }

    /** Single-element dataset */
    private static double[] buildSingleElement() {
        return new double[]{ Math.PI };
    }

    /** Empty dataset (boundary case) */
    private static double[] buildEmptyDataset() {
        return new double[0];
    }

    /** Sparse mixed-sign dataset */
    private static double[] buildMixedSign() {
        return new double[]{
            -100.0, -50.5, -1.0, 0.0, 1.0, 50.5, 100.0, -75.3, 88.8, -0.001
        };
    }

    // ---------------------------------------------------------------
    // Formatting helpers
    // ---------------------------------------------------------------

    private static String fmt(double v) {
        return Double.isNaN(v) ? "NaN" : String.format("%.6f", v);
    }

    private static String arrayPreview(double[] d, int max) {
        if (d.length == 0) return "(empty)";
        int n = Math.min(d.length, max);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < n; i++) {
            sb.append(fmt(d[i]));
            if (i < n - 1) sb.append(", ");
        }
        if (d.length > max) sb.append(", … (").append(d.length).append(" total)");
        sb.append("]");
        return sb.toString();
    }

    private static void printHistogram(int[] hist, double min, double max) {
        if (hist == null || hist.length == 0) return;
        double binWidth = (max - min) / hist.length;
        int maxCount = 0;
        for (int c : hist) if (c > maxCount) maxCount = c;
        final int BAR_WIDTH = 30;
        for (int i = 0; i < hist.length; i++) {
            double lo = min + i * binWidth;
            double hi = lo + binWidth;
            int bar = (maxCount > 0) ? (hist[i] * BAR_WIDTH / maxCount) : 0;
            System.out.printf("│   [%8.2f – %8.2f) │ %-30s │ %d%n",
                    lo, hi, "█".repeat(bar), hist[i]);
        }
    }

    // ---------------------------------------------------------------
    // Entry point
    // ---------------------------------------------------------------
    public static void main(String[] args) {

        NumericalAnalyzer analyzer = new NumericalAnalyzer();

        // ── Dataset catalogue ────────────────────────────────────────
        String[]   names    = {
            "Linear Ramp (1–100)",
            "Gaussian-like (N≈50,σ≈10, n=200)",
            "Dirty (NaN / Inf)",
            "Constant (42.0 × 10)",
            "Single Element (π)",
            "Empty",
            "Mixed-Sign"
        };
        double[][] datasets = {
            buildLinearRamp(),
            buildGaussianLike(),
            buildDirtyDataset(),
            buildConstantDataset(),
            buildSingleElement(),
            buildEmptyDataset(),
            buildMixedSign()
        };

        // Validation bounds per dataset (lo, hi)
        double[][] bounds = {
            { 0.0,   100.0 },
            { 0.0,   100.0 },
            { 0.0,    10.0 },
            { 40.0,   45.0 },
            { 3.0,     4.0 },
            { 0.0,     1.0 },
            {-100.0, 100.0 }
        };

        final int HIST_BINS = 5;

        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║           JNI Numerical Dataset Analyzer Demo                ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");

        for (int idx = 0; idx < datasets.length; idx++) {
            double[] data = datasets[idx];
            System.out.println("\n┌──────────────────────────────────────────────────────────────");
            System.out.printf ("│ Dataset #%d — %s%n", idx + 1, names[idx]);
            System.out.printf ("│ Preview : %s%n", arrayPreview(data, 6));
            System.out.println("├──────────────────────────────────────────────────────────────");

            // ── ① Aggregate statistics ─────────────────────────────────
            double[] stats = analyzer.aggregateStats(data);
            System.out.println("│");
            System.out.println("│ [AGGREGATE STATS]");
            System.out.printf ("│   Min      : %s%n", fmt(stats[0]));
            System.out.printf ("│   Max      : %s%n", fmt(stats[1]));
            System.out.printf ("│   Sum      : %s%n", fmt(stats[2]));
            System.out.printf ("│   Mean     : %s%n", fmt(stats[3]));
            System.out.printf ("│   Variance : %s%n", fmt(stats[4]));
            System.out.printf ("│   Std Dev  : %s%n", fmt(stats[5]));

            // ── ② Validation ───────────────────────────────────────────
            double lo = bounds[idx][0], hi = bounds[idx][1];
            int[] validation = analyzer.validateDataset(data, lo, hi);
            System.out.println("│");
            System.out.printf ("│ [VALIDATION]  range = [%.2f, %.2f]%n", lo, hi);
            System.out.printf ("│   NaN count        : %d%n", validation[0]);
            System.out.printf ("│   Inf count        : %d%n", validation[1]);
            System.out.printf ("│   Out-of-range     : %d%n", validation[2]);
            System.out.printf ("│   Valid count      : %d%n", validation[3]);

            // ── ③ Normalisation ────────────────────────────────────────
            double[] normalised = analyzer.normalizeDataset(data);
            System.out.println("│");
            System.out.println("│ [NORMALISE → [0,1]]");
            System.out.printf ("│   Result preview : %s%n", arrayPreview(normalised, 6));

            // Verify bounds (ignoring NaN/Inf in dirty dataset)
            boolean normOk = true;
            for (double v : normalised) {
                if (!Double.isNaN(v) && !Double.isInfinite(v) && (v < -1e-9 || v > 1 + 1e-9)) {
                    normOk = false; break;
                }
            }
            System.out.printf ("│   Bounds check   : %s%n", normOk ? "✓ PASS" : "✗ FAIL");

            // ── ④ Histogram ────────────────────────────────────────────
            if (data.length >= 2) {
                int[] hist = analyzer.histogramDataset(data, HIST_BINS);
                System.out.println("│");
                System.out.printf ("│ [HISTOGRAM]  bins=%d%n", HIST_BINS);
                printHistogram(hist, stats[0], stats[1]);

                // Verify total count matches data length
                int total = 0;
                for (int c : hist) total += c;
                System.out.printf ("│   Count check    : %d / %d  %s%n",
                        total, data.length, total == data.length ? "✓ PASS" : "✗ FAIL");
            } else {
                System.out.println("│ [HISTOGRAM]  skipped (< 2 elements)");
            }
        }

        // ── ⑤ Dot-product demo ─────────────────────────────────────────
        System.out.println("\n┌──────────────────────────────────────────────────────────────");
        System.out.println("│ Dot-Product Demo");
        System.out.println("├──────────────────────────────────────────────────────────────");

        double[] vecA = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] vecB = { 5.0, 4.0, 3.0, 2.0, 1.0 };
        double   dp   = analyzer.dotProduct(vecA, vecB);

        System.out.printf("│ A = %s%n", arrayPreview(vecA, vecA.length));
        System.out.printf("│ B = %s%n", arrayPreview(vecB, vecB.length));
        System.out.printf("│ A · B = %.6f  (expected 35.000000)  %s%n",
                dp, Math.abs(dp - 35.0) < 1e-9 ? "✓ PASS" : "✗ FAIL");

        System.out.println("\n└──────────────────────────────────────────────────────────────");
        System.out.println("All datasets analysed successfully.");
    }
}