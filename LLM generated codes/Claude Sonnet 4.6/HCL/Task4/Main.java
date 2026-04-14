package com.solver;

import java.util.Arrays;

/**
 * Demonstrates the full JNI solver lifecycle:
 * <ol>
 *   <li>Library loading (automatic via static initialiser in {@link NativeSolver}).</li>
 *   <li>Initialisation with convergence parameters.</li>
 *   <li>Solving two representative linear systems.</li>
 *   <li>Safe resource cleanup via try-with-resources.</li>
 * </ol>
 */
public final class Main {

    public static void main(String[] args) {
        System.out.println("=== JNI Native Solver Demo ===");
        System.out.println("Platform: " + Platform.current());
        System.out.println();

        try (NativeSolver solver = new NativeSolver()) {

            // ── 1. Initialise ─────────────────────────────────────────────
            solver.initialize(/* maxIterations */ 1000, /* tolerance */ 1e-9);
            System.out.println("Solver version : " + solver.getSolverVersion());
            System.out.println();

            // ── 2. Solve a 3×3 system ─────────────────────────────────────
            //
            //   [ 4  -2   1 ] [ x ]   [  3 ]
            //   [ 2   5  -1 ] [ y ] = [ 10 ]
            //   [-1   2   6 ] [ z ]   [ 16 ]
            //
            //   Exact solution: x ≈ 1.0,  y ≈ 2.0,  z ≈ 3.0

            double[][] A1 = {
                { 4, -2,  1 },
                { 2,  5, -1 },
                {-1,  2,  6 }
            };
            double[] b1 = { 3, 10, 16 };

            System.out.println("--- Test 1: 3×3 system ---");
            printSystem(A1, b1);
            SolverResult result1 = solver.solve(A1, b1);
            printResult(result1);
            assertConverged(result1);

            // ── 3. Solve a 4×4 system ─────────────────────────────────────
            //
            //   Diagonally dominant → guaranteed convergence

            double[][] A2 = {
                {10,  1,  0,  0},
                { 1, 10,  1,  0},
                { 0,  1, 10,  1},
                { 0,  0,  1, 10}
            };
            double[] b2 = {11, 12, 12, 11};

            System.out.println("--- Test 2: 4×4 diagonally dominant system ---");
            printSystem(A2, b2);
            SolverResult result2 = solver.solve(A2, b2);
            printResult(result2);
            assertConverged(result2);

        } catch (SolverException e) {
            System.err.println("FATAL – Solver error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("FATAL – Unexpected error: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(2);
        }

        System.out.println();
        System.out.println("=== All tests passed ===");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void printSystem(double[][] A, double[] b) {
        int n = b.length;
        System.out.println("  Coefficient matrix A (" + n + "×" + n + "):");
        for (double[] row : A) {
            System.out.println("    " + Arrays.toString(row));
        }
        System.out.println("  RHS vector b: " + Arrays.toString(b));
    }

    private static void printResult(SolverResult r) {
        System.out.println("  Result       : " + r);
    }

    private static void assertConverged(SolverResult r) {
        if (!r.isConverged()) {
            throw new SolverException(
                "Solver did not converge after " + r.getIterations()
                + " iterations. Residual: " + r.getResidual()
            );
        }
    }
}