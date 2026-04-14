package com.mathengine;

import java.util.Arrays;

/**
 * Demonstrates every operation exposed by {@link MathEngine}.
 */
public final class Main {

    public static void main(String[] args) {

        System.out.println("╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║          JNI Advanced Math Engine — Demonstration           ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");

        try (MathEngine engine = new MathEngine()) {

            // ── Arithmetic ────────────────────────────────────────────────────
            section("ARITHMETIC");
            print(engine.power(2.0, 10.0));
            print(engine.power(3.0, 0.5));
            print(engine.sqrt(144.0));
            print(engine.sqrt(2.0));

            // ── Trigonometry ──────────────────────────────────────────────────
            section("TRIGONOMETRY");
            print(engine.sin(Math.PI / 6));   // expected 0.5
            print(engine.cos(Math.PI / 3));   // expected 0.5
            print(engine.tan(Math.PI / 4));   // expected 1.0
            print(engine.sin(0));
            print(engine.cos(Math.PI));       // expected -1.0

            // ── Calculus ──────────────────────────────────────────────────────
            section("CALCULUS — Derivative & Integral");
            print(engine.derivative(0.0,       1e-5));  // d/dx sin(x) at 0 ≈ cos(0) = 1
            print(engine.derivative(Math.PI/4, 1e-5));  // ≈ cos(π/4) ≈ 0.7071
            print(engine.integral(0, Math.PI, 1000));   // ∫sin(x)dx [0,π] = 2
            print(engine.integral(0, Math.PI / 2, 1000));// = 1

            // ── Linear Algebra ────────────────────────────────────────────────
            section("LINEAR ALGEBRA");
            double[] u = {1, 2, 3};
            double[] v = {4, 5, 6};
            print(engine.dotProduct(u, v));             // 1*4+2*5+3*6 = 32
            print(engine.normalizeVector(u));            // unit vector of [1,2,3]

            // 2×2 matrix multiply:  [1 2][5 6]   [19 22]
            //                       [3 4][7 8] = [43 50]
            double[] A = {1, 2, 3, 4};
            double[] B = {5, 6, 7, 8};
            print(engine.matrixMultiply(A, B, 2));

            // 3×3 identity × arbitrary matrix → same matrix
            double[] I3 = {1,0,0, 0,1,0, 0,0,1};
            double[] M3 = {9,8,7, 6,5,4, 3,2,1};
            print(engine.matrixMultiply(I3, M3, 3));

            // ── Statistics ────────────────────────────────────────────────────
            section("STATISTICS");
            double[] dataset = {2, 4, 4, 4, 5, 5, 7, 9};
            print(engine.mean(dataset));                 // 5.0
            print(engine.variance(dataset));             // 4.0
            print(engine.stdDev(dataset));               // 2.0

            // ── Number Theory ─────────────────────────────────────────────────
            section("NUMBER THEORY");
            print(engine.gcd(48, 18));                   // 6
            print(engine.gcd(1071, 462));                // 21
            print(engine.isPrime(7919));                 // PRIME
            print(engine.isPrime(7920));                 // COMPOSITE
            print(engine.isPrime(2));                    // PRIME
            print(engine.isPrime(1));                    // COMPOSITE
        }

        System.out.println("\n[Main] All computations completed successfully.");
    }

    private static void section(String title) {
        System.out.printf("%n── %-40s ──────────────────%n", title);
    }

    private static void print(MathResult r) {
        System.out.println("  " + r);
    }
}