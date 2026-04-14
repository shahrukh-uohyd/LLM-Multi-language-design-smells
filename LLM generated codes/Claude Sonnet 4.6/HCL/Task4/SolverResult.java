package com.solver;

import java.util.Arrays;

/**
 * Immutable value object that carries the outcome of a native solver call.
 *
 * <ul>
 *   <li>{@code solution}    – the computed solution vector</li>
 *   <li>{@code residual}    – measure of how closely the solution satisfies the system</li>
 *   <li>{@code iterations}  – number of solver iterations performed</li>
 *   <li>{@code converged}   – whether the solver reached the convergence threshold</li>
 * </ul>
 */
public final class SolverResult {

    private final double[] solution;
    private final double   residual;
    private final int      iterations;
    private final boolean  converged;

    public SolverResult(double[] solution, double residual, int iterations, boolean converged) {
        this.solution   = solution.clone();   // defensive copy
        this.residual   = residual;
        this.iterations = iterations;
        this.converged  = converged;
    }

    /** Returns a defensive copy of the solution vector. */
    public double[] getSolution()   { return solution.clone(); }
    public double   getResidual()   { return residual;         }
    public int      getIterations() { return iterations;       }
    public boolean  isConverged()   { return converged;        }

    @Override
    public String toString() {
        return String.format(
            "SolverResult{converged=%b, iterations=%d, residual=%.6e, solution=%s}",
            converged, iterations, residual, Arrays.toString(solution)
        );
    }
}