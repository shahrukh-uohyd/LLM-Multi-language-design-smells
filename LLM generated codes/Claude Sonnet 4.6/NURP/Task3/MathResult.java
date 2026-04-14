package com.mathengine;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable value object that carries the result of a native mathematical
 * computation along with the operation name and elapsed time.
 */
public final class MathResult {

    private final String   operation;
    private final double   scalarResult;
    private final double[] vectorResult;
    private final long     elapsedNanos;
    private final String   notes;

    // ------------------------------------------------------------------
    // Factory constructors
    // ------------------------------------------------------------------

    /** Creates a scalar result. */
    public static MathResult scalar(String operation, double value, long nanos) {
        return new MathResult(operation, value, null, nanos, "");
    }

    /** Creates a vector result. */
    public static MathResult vector(String operation, double[] values, long nanos) {
        return new MathResult(operation, Double.NaN, values.clone(), nanos, "");
    }

    /** Creates a scalar result with additional notes. */
    public static MathResult scalarWithNotes(String op, double value, long nanos, String notes) {
        return new MathResult(op, value, null, nanos, notes);
    }

    private MathResult(String operation, double scalarResult,
                       double[] vectorResult, long elapsedNanos, String notes) {
        this.operation    = Objects.requireNonNull(operation);
        this.scalarResult = scalarResult;
        this.vectorResult = vectorResult;
        this.elapsedNanos = elapsedNanos;
        this.notes        = notes == null ? "" : notes;
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public String   getOperation()    { return operation; }
    public double   getScalarResult() { return scalarResult; }
    public double[] getVectorResult() { return vectorResult == null ? null : vectorResult.clone(); }
    public long     getElapsedNanos() { return elapsedNanos; }
    public String   getNotes()        { return notes; }
    public boolean  isVector()        { return vectorResult != null; }

    // ------------------------------------------------------------------
    // Display
    // ------------------------------------------------------------------

    @Override
    public String toString() {
        String resultStr = isVector()
                ? Arrays.toString(vectorResult)
                : String.format("%.10f", scalarResult);

        return String.format("%-30s │ result = %-30s │ %,d ns%s",
                operation, resultStr, elapsedNanos,
                notes.isEmpty() ? "" : " │ " + notes);
    }
}