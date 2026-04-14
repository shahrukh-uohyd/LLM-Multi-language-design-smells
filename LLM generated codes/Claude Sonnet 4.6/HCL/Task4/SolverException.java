package com.solver;

/**
 * Thrown when any solver operation fails — either during native library
 * loading or during a solver computation.
 */
public final class SolverException extends RuntimeException {

    public SolverException(String message) {
        super(message);
    }

    public SolverException(String message, Throwable cause) {
        super(message, cause);
    }
}