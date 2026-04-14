package com.app.config;

/**
 * Thrown when a native configuration operation fails.
 *
 * <p>{@link #getErrorCode()} exposes the raw native error code (C {@code errno}
 * or an internal config error) for diagnostics. {@link #getOperation()} identifies
 * exactly which config operation triggered the failure.</p>
 *
 * <p>This is an unchecked exception — callers that cannot sensibly recover
 * (e.g. a missing mandatory config file) are not forced to declare it, while
 * callers that need fine-grained control can catch it explicitly.</p>
 */
public class ConfigException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Identifies the config operation that failed.
     * Ordinal values must stay in sync with {@code OP_*} constants
     * in {@code config_native.c}.
     */
    public enum Operation {
        LOAD,
        SAVE,
        GET_VALUE,
        SET_VALUE,
        DELETE_KEY,
        VALIDATE,
        RELOAD,
        UNKNOWN
    }

    private final int       errorCode;
    private final Operation operation;

    // ── constructors ──────────────────────────────────────────────────

    public ConfigException(String message) {
        super(message);
        this.errorCode = 0;
        this.operation = Operation.UNKNOWN;
    }

    public ConfigException(String message, Operation operation) {
        super(message);
        this.errorCode = 0;
        this.operation = operation;
    }

    /**
     * Primary constructor — called from native code via JNI.
     *
     * @param message          human-readable description
     * @param errorCode        native error code (errno or internal code)
     * @param operationOrdinal ordinal of the failing {@link Operation}
     */
    public ConfigException(String message, int errorCode, int operationOrdinal) {
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

    public ConfigException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = 0;
        this.operation = Operation.UNKNOWN;
    }

    // ── accessors ─────────────────────────────────────────────────────

    /** Returns the raw native error code, or {@code 0} if not available. */
    public int getErrorCode() { return errorCode; }

    /** Returns the config operation that triggered this exception. */
    public Operation getOperation() { return operation; }
}