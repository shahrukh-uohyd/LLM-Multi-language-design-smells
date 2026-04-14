package com.solver;

import java.util.Objects;

/**
 * Java facade that exposes the native solver over JNI.
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *   <li>Instantiate {@code NativeSolver}.</li>
 *   <li>Call {@link #initialize(int, double)} once.</li>
 *   <li>Call {@link #solve(double[][], double[])} one or more times.</li>
 *   <li>Call {@link #shutdown()} to release native resources.</li>
 * </ol>
 *
 * <p>The class implements {@link AutoCloseable} so it can be used in a
 * try-with-resources block for safe resource management.</p>
 *
 * <h2>Native method mapping</h2>
 * The C symbol names expected by the JVM follow the standard JNI naming
 * convention: {@code Java_com_solver_NativeSolver_<methodName>}.
 *
 * <pre>
 *  Java method             ↔  C function
 *  ─────────────────────────────────────────────────────────────
 *  nativeInitialize(…)     ↔  Java_com_solver_NativeSolver_nativeInitialize
 *  nativeSolve(…)          ↔  Java_com_solver_NativeSolver_nativeSolve
 *  nativeGetLastResidual() ↔  Java_com_solver_NativeSolver_nativeGetLastResidual
 *  nativeGetIterations()   ↔  Java_com_solver_NativeSolver_nativeGetIterations
 *  nativeShutdown()        ↔  Java_com_solver_NativeSolver_nativeShutdown
 * </pre>
 */
public final class NativeSolver implements AutoCloseable {

    // -------------------------------------------------------------------------
    // Library loading — triggered once when the class is first referenced
    // -------------------------------------------------------------------------
    static {
        NativeLibraryLoader.load();
    }

    // -------------------------------------------------------------------------
    // Instance state
    // -------------------------------------------------------------------------
    private volatile boolean initialized = false;
    private volatile boolean shutdown    = false;

    /** Native handle/pointer managed by the C layer (opaque to Java). */
    private long nativeHandle = 0L;

    // -------------------------------------------------------------------------
    // Public Java API
    // -------------------------------------------------------------------------

    /**
     * Initialises the solver engine.
     *
     * @param maxIterations maximum number of solver iterations permitted
     * @param tolerance     convergence tolerance (e.g. {@code 1e-9})
     * @throws SolverException      if initialisation fails natively
     * @throws IllegalStateException if already initialised or shut down
     */
    public synchronized void initialize(int maxIterations, double tolerance) {
        checkNotShutdown();
        if (initialized) {
            throw new IllegalStateException("Solver is already initialised.");
        }
        if (maxIterations < 1) {
            throw new IllegalArgumentException("maxIterations must be >= 1, got: " + maxIterations);
        }
        if (tolerance <= 0.0) {
            throw new IllegalArgumentException("tolerance must be > 0.0, got: " + tolerance);
        }

        nativeHandle = nativeInitialize(maxIterations, tolerance);
        if (nativeHandle == 0L) {
            throw new SolverException("Native solver initialisation returned a null handle.");
        }
        initialized = true;
        System.out.printf("[NativeSolver] Initialized — maxIterations=%d, tolerance=%.2e%n",
                maxIterations, tolerance);
    }

    /**
     * Solves the linear system {@code A·x = b}.
     *
     * @param A coefficient matrix (n×n, row-major)
     * @param b right-hand side vector (length n)
     * @return {@link SolverResult} containing the solution vector and diagnostics
     * @throws SolverException       if the native solver reports an error
     * @throws IllegalArgumentException if dimensions are inconsistent
     * @throws IllegalStateException    if not initialised or already shut down
     */
    public synchronized SolverResult solve(double[][] A, double[] b) {
        checkReady();
        validateInputs(A, b);

        int    n       = b.length;
        double[] flatA = flattenMatrix(A, n);

        double[] solution   = new double[n];
        boolean  converged  = nativeSolve(nativeHandle, flatA, b, solution, n);

        double residual    = nativeGetLastResidual(nativeHandle);
        int    iterations  = nativeGetIterations(nativeHandle);

        return new SolverResult(solution, residual, iterations, converged);
    }

    /**
     * Returns the version string of the underlying native solver library.
     *
     * @throws IllegalStateException if not initialised or shut down
     */
    public synchronized String getSolverVersion() {
        checkReady();
        return nativeGetVersion(nativeHandle);
    }

    /**
     * Releases all native resources. After calling this method the
     * instance is no longer usable.
     */
    @Override
    public synchronized void close() {
        shutdown();
    }

    /**
     * Shuts down the native solver and frees all associated resources.
     * Idempotent — safe to call more than once.
     */
    public synchronized void shutdown() {
        if (shutdown) return;
        if (initialized && nativeHandle != 0L) {
            nativeShutdown(nativeHandle);
            nativeHandle = 0L;
        }
        shutdown = true;
        System.out.println("[NativeSolver] Shutdown complete.");
    }

    // -------------------------------------------------------------------------
    // Native method declarations (implemented in solver.cpp / solver.h)
    // -------------------------------------------------------------------------

    /**
     * Initialises the native solver engine.
     *
     * @return opaque native handle (must be non-zero on success)
     */
    private native long nativeInitialize(int maxIterations, double tolerance);

    /**
     * Solves {@code A·x = b} and writes the solution into {@code solution}.
     *
     * @param handle   opaque native handle from {@link #nativeInitialize}
     * @param flatA    coefficient matrix flattened in row-major order (length n*n)
     * @param b        right-hand side vector (length n)
     * @param solution output array to be filled with the solution (length n)
     * @param n        dimension of the system
     * @return {@code true} if the solver converged, {@code false} otherwise
     */
    private native boolean nativeSolve(long handle, double[] flatA, double[] b,
                                       double[] solution, int n);

    /**
     * Returns the residual norm from the most recent solve.
     *
     * @param handle opaque native handle
     */
    private native double nativeGetLastResidual(long handle);

    /**
     * Returns the iteration count from the most recent solve.
     *
     * @param handle opaque native handle
     */
    private native int nativeGetIterations(long handle);

    /**
     * Returns the native library version string.
     *
     * @param handle opaque native handle
     */
    private native String nativeGetVersion(long handle);

    /**
     * Shuts down the native solver and frees all resources.
     *
     * @param handle opaque native handle
     */
    private native void nativeShutdown(long handle);

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private void checkNotShutdown() {
        if (shutdown) throw new IllegalStateException("Solver has been shut down.");
    }

    private void checkReady() {
        checkNotShutdown();
        if (!initialized) throw new IllegalStateException("Solver is not initialised. Call initialize() first.");
    }

    private static void validateInputs(double[][] A, double[] b) {
        Objects.requireNonNull(A, "Coefficient matrix A must not be null.");
        Objects.requireNonNull(b, "Right-hand side vector b must not be null.");
        int n = b.length;
        if (n == 0) throw new IllegalArgumentException("System dimension must be > 0.");
        if (A.length != n) {
            throw new IllegalArgumentException(
                    "Matrix A must be square n×n. Expected " + n + " rows, got " + A.length + ".");
        }
        for (int i = 0; i < n; i++) {
            if (A[i] == null || A[i].length != n) {
                throw new IllegalArgumentException(
                        "Matrix A row " + i + " must have length " + n + ".");
            }
        }
    }

    /**
     * Flattens a 2-D row-major matrix to a 1-D array for JNI transfer.
     */
    private static double[] flattenMatrix(double[][] A, int n) {
        double[] flat = new double[n * n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(A[i], 0, flat, i * n, n);
        }
        return flat;
    }
}