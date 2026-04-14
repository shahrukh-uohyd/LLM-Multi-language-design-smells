package com.example.dbridge.model;

/**
 * Describes the ACID commit outcome for a single {@link TransactionRecord}.
 *
 * <p>Constructed by the native layer and returned to Java through the JNI
 * bridge after each individual {@code commitToStorage()} call.
 */
public final class CommitResult {

    // ── Native commit-status codes (mirror native_storage.h) ─────────────────
    public static final int COMMIT_OK               = 0;
    public static final int COMMIT_ERR_DUPLICATE    = 1;  // record already exists
    public static final int COMMIT_ERR_CONSTRAINT   = 2;  // integrity violation
    public static final int COMMIT_ERR_IO           = 3;  // disk / fs failure
    public static final int COMMIT_ERR_SERIALISE    = 4;  // bad field encoding
    public static final int COMMIT_ERR_INTERNAL     = 5;  // unexpected native error

    private final String  transactionId;
    private final boolean committed;
    private final int     commitStatusCode;
    private final String  nativeErrorMessage;
    private final long    commitTimestampMs;   // wall-clock time of the commit

    /**
     * Called reflectively by the native layer via JNI.
     * Field order must match the C++ {@code NewObject()} call exactly.
     */
    public CommitResult(String  transactionId,
                        boolean committed,
                        int     commitStatusCode,
                        String  nativeErrorMessage,
                        long    commitTimestampMs) {
        this.transactionId      = transactionId;
        this.committed          = committed;
        this.commitStatusCode   = commitStatusCode;
        this.nativeErrorMessage = nativeErrorMessage;
        this.commitTimestampMs  = commitTimestampMs;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────
    public String  getTransactionId()      { return transactionId;      }
    public boolean isCommitted()           { return committed;           }
    public int     getCommitStatusCode()   { return commitStatusCode;    }
    public String  getNativeErrorMessage() { return nativeErrorMessage;  }
    public long    getCommitTimestampMs()  { return commitTimestampMs;   }

    @Override
    public String toString() {
        return String.format("CommitResult{id='%s', committed=%b, code=%d, msg='%s'}",
                transactionId, committed, commitStatusCode, nativeErrorMessage);
    }
}