package com.app.signal;

/**
 * Thrown when a native signal processing operation fails.
 *
 * <p>{@link #getErrorCode()} exposes the raw native error code for
 * diagnostic and logging purposes. {@link #getOperation()} identifies
 * which DSP operation triggered the failure.</p>
 *
 * <p>This is an unchecked exception — callers that cannot recover
 * (e.g. an invalid signal buffer) are not forced to declare it,
 * while callers that need fine-grained control can catch it.</p>
 */
public class SignalProcessingException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Identifies the DSP operation that failed.
     * Ordinal values must stay in sync with {@code OP_*} constants
     * in {@code signal_processor_native.cpp}.
     */
    public enum Operation {
        FFT,
        IFFT,
        FILTER,
        RESAMPLE,
        STATISTICS,
        CONVOLUTION,
        UNKNOWN
    }

    private final int       errorCode;
    private final Operation operation;

    // ── constructors ──────────────────────────────────────────────────

    public SignalProcessingException(String message) {
        super(message);
        this.errorCode = 0;
        this.operation = Operation.UNKNOWN;
    }

    public SignalProcessingException(String message, Operation operation) {
        super(message);
        this.errorCode = 0;
        this.operation = operation;
    }

    /**
     * Primary constructor — called from native code via JNI.
     *
     * @param message          human-readable description
     * @param errorCode        native error code (errno or internal DSP code)
     * @param operationOrdinal ordinal of the failing {@link Operation}
     */
    public SignalProcessingException(String message, int errorCode, int operationOrdinal) {
        super(message + (errorCode != 0 ? " [native error code: " + errorCode + "]" : ""));
        this.errorCode = errorCode;
        Operation op;
        try {
            op = Operation.values()[operationOrdinal];
        } catch (ArrayIndexOutOfBoundsException e) {
            op = Operation.UNKNOWN;
        }
        this.operation = op;
    }

    public SignalProcessingException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = 0;
        this.operation = Operation.UNKNOWN;
    }

    // ── accessors ─────────────────────────────────────────────────────

    /** Returns the raw native error code, or {@code 0} if not available. */
    public int getErrorCode() { return errorCode; }

    /** Returns the DSP operation that triggered this exception. */
    public Operation getOperation() { return operation; }
}