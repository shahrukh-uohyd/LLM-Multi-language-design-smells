package com.mathengine;

/**
 * JNI bridge to the {@code math_engine} native library.
 *
 * <h2>Operations Provided</h2>
 * <ul>
 *   <li><b>Arithmetic:</b> power, integer square-root via Newton-Raphson</li>
 *   <li><b>Trigonometry:</b> sin, cos, tan with custom precision</li>
 *   <li><b>Calculus:</b> numerical derivative and definite integral (Simpson's Rule)</li>
 *   <li><b>Linear Algebra:</b> dot product, vector normalisation, matrix multiply (n×n)</li>
 *   <li><b>Statistics:</b> mean, variance, standard deviation</li>
 *   <li><b>Number Theory:</b> GCD (Euclidean), primality test (Miller-Rabin)</li>
 * </ul>
 *
 * <p>All {@code native} methods are declared {@code private} and wrapped by public
 * methods that time the call and return a {@link MathResult}.
 */
public final class MathEngine implements AutoCloseable {

    // Load the native library exactly once when this class is first used.
    static {
        NativeLibraryLoader.load();
    }

    // =========================================================================
    // Native method declarations  (implemented in math_engine.c)
    // =========================================================================

    // --- Arithmetic ----------------------------------------------------------
    private native double nativePower(double base, double exponent);
    private native double nativeSqrt(double value);

    // --- Trigonometry --------------------------------------------------------
    private native double nativeSin(double radians);
    private native double nativeCos(double radians);
    private native double nativeTan(double radians);

    // --- Calculus ------------------------------------------------------------
    /** Numerical derivative of sin(x) at the given point using central differences. */
    private native double nativeDerivative(double x, double h);

    /** Definite integral of sin(x) from a to b using composite Simpson's Rule. */
    private native double nativeIntegral(double a, double b, int n);

    // --- Linear Algebra ------------------------------------------------------
    private native double   nativeDotProduct(double[] a, double[] b);
    private native double[] nativeNormalizeVector(double[] v);

    /**
     * Multiplies two square matrices of size {@code size × size}.
     * Both matrices are passed as flat row-major arrays of length {@code size*size}.
     *
     * @return flat row-major result array.
     */
    private native double[] nativeMatrixMultiply(double[] a, double[] b, int size);

    // --- Statistics ----------------------------------------------------------
    private native double nativeMean(double[] data);
    private native double nativeVariance(double[] data);
    private native double nativeStdDev(double[] data);

    // --- Number Theory -------------------------------------------------------
    private native long    nativeGcd(long a, long b);
    private native boolean nativeIsPrime(long n);

    // =========================================================================
    // Public API  (measures elapsed time, returns MathResult)
    // =========================================================================

    // --- Arithmetic ----------------------------------------------------------

    /** Computes {@code base ^ exponent} using native fast-power. */
    public MathResult power(double base, double exponent) {
        long t0 = System.nanoTime();
        double r = nativePower(base, exponent);
        return MathResult.scalar("power(" + base + ", " + exponent + ")", r,
                                 System.nanoTime() - t0);
    }

    /** Computes √value using native Newton-Raphson iteration. */
    public MathResult sqrt(double value) {
        if (value < 0) throw new IllegalArgumentException("sqrt requires non-negative input.");
        long t0 = System.nanoTime();
        double r = nativeSqrt(value);
        return MathResult.scalar("sqrt(" + value + ")", r, System.nanoTime() - t0);
    }

    // --- Trigonometry --------------------------------------------------------

    /** Computes sin(radians) using native Taylor-series expansion. */
    public MathResult sin(double radians) {
        long t0 = System.nanoTime();
        double r = nativeSin(radians);
        return MathResult.scalar("sin(" + radians + ")", r, System.nanoTime() - t0);
    }

    /** Computes cos(radians) using native Taylor-series expansion. */
    public MathResult cos(double radians) {
        long t0 = System.nanoTime();
        double r = nativeCos(radians);
        return MathResult.scalar("cos(" + radians + ")", r, System.nanoTime() - t0);
    }

    /** Computes tan(radians) = sin/cos using native code. */
    public MathResult tan(double radians) {
        long t0 = System.nanoTime();
        double r = nativeTan(radians);
        return MathResult.scalar("tan(" + radians + ")", r, System.nanoTime() - t0);
    }

    // --- Calculus ------------------------------------------------------------

    /**
     * Estimates the derivative of sin(x) at {@code x} using central differences
     * with step size {@code h}.
     */
    public MathResult derivative(double x, double h) {
        long t0 = System.nanoTime();
        double r = nativeDerivative(x, h);
        return MathResult.scalarWithNotes(
                "d/dx sin(x) at x=" + x, r, System.nanoTime() - t0,
                "h=" + h + ", expected≈" + String.format("%.6f", Math.cos(x)));
    }

    /**
     * Numerically integrates sin(x) from {@code a} to {@code b} using
     * composite Simpson's Rule with {@code n} sub-intervals (must be even).
     */
    public MathResult integral(double a, double b, int n) {
        if (n % 2 != 0) throw new IllegalArgumentException("n must be even for Simpson's Rule.");
        long t0 = System.nanoTime();
        double r = nativeIntegral(a, b, n);
        double exact = -Math.cos(b) + Math.cos(a);
        return MathResult.scalarWithNotes(
                "∫sin(x)dx [" + a + "," + b + "]", r, System.nanoTime() - t0,
                "n=" + n + ", exact=" + String.format("%.10f", exact));
    }

    // --- Linear Algebra ------------------------------------------------------

    /** Computes the dot product of two equal-length vectors. */
    public MathResult dotProduct(double[] a, double[] b) {
        if (a.length != b.length)
            throw new IllegalArgumentException("Vectors must have equal length.");
        long t0 = System.nanoTime();
        double r = nativeDotProduct(a, b);
        return MathResult.scalar("dotProduct", r, System.nanoTime() - t0);
    }

    /** Normalises a vector to unit length. */
    public MathResult normalizeVector(double[] v) {
        long t0 = System.nanoTime();
        double[] r = nativeNormalizeVector(v);
        return MathResult.vector("normalizeVector", r, System.nanoTime() - t0);
    }

    /**
     * Multiplies two square {@code size × size} matrices.
     *
     * @param a    flat row-major matrix A (length = size*size)
     * @param b    flat row-major matrix B (length = size*size)
     * @param size dimension of both matrices
     */
    public MathResult matrixMultiply(double[] a, double[] b, int size) {
        if (a.length != size * size || b.length != size * size)
            throw new IllegalArgumentException("Matrix dimensions mismatch.");
        long t0 = System.nanoTime();
        double[] r = nativeMatrixMultiply(a, b, size);
        return MathResult.vector("matrixMultiply(" + size + "x" + size + ")", r,
                                 System.nanoTime() - t0);
    }

    // --- Statistics ----------------------------------------------------------

    /** Computes the arithmetic mean of the data array. */
    public MathResult mean(double[] data) {
        requireNonEmpty(data, "mean");
        long t0 = System.nanoTime();
        double r = nativeMean(data);
        return MathResult.scalar("mean", r, System.nanoTime() - t0);
    }

    /** Computes the population variance of the data array. */
    public MathResult variance(double[] data) {
        requireNonEmpty(data, "variance");
        long t0 = System.nanoTime();
        double r = nativeVariance(data);
        return MathResult.scalar("variance", r, System.nanoTime() - t0);
    }

    /** Computes the population standard deviation of the data array. */
    public MathResult stdDev(double[] data) {
        requireNonEmpty(data, "stdDev");
        long t0 = System.nanoTime();
        double r = nativeStdDev(data);
        return MathResult.scalar("stdDev", r, System.nanoTime() - t0);
    }

    // --- Number Theory -------------------------------------------------------

    /** Computes the GCD of {@code a} and {@code b} using the Euclidean algorithm. */
    public MathResult gcd(long a, long b) {
        long t0 = System.nanoTime();
        long r = nativeGcd(Math.abs(a), Math.abs(b));
        return MathResult.scalar("gcd(" + a + ", " + b + ")", r, System.nanoTime() - t0);
    }

    /** Tests primality of {@code n} using the Miller-Rabin probabilistic test. */
    public MathResult isPrime(long n) {
        long t0 = System.nanoTime();
        boolean r = nativeIsPrime(n);
        return MathResult.scalarWithNotes(
                "isPrime(" + n + ")", r ? 1.0 : 0.0, System.nanoTime() - t0,
                r ? "PRIME" : "COMPOSITE");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void requireNonEmpty(double[] data, String op) {
        if (data == null || data.length == 0)
            throw new IllegalArgumentException(op + " requires a non-empty array.");
    }

    /** No-op; present so {@code MathEngine} can be used in try-with-resources. */
    @Override
    public void close() {}
}