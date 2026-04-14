package com.example.dbridge.jni;

import com.example.dbridge.model.CommitResult;
import com.example.dbridge.model.SyncReport;
import com.example.dbridge.model.TransactionRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JNI bridge that persists {@link TransactionRecord} objects to the native
 * local storage one-by-one, guaranteeing per-row ACID compliance.
 *
 * <h3>Commit strategy — why per-record?</h3>
 * <ul>
 *   <li>Each record is committed inside its own {@code BEGIN … COMMIT}
 *       transaction in C++.  A failure on record <em>N</em> never rolls back
 *       records 1…N-1 that already succeeded.</li>
 *   <li>The Java layer collects every {@link CommitResult} and builds a
 *       {@link SyncReport} that exposes per-record success / failure detail.</li>
 *   <li>Failed records are surfaced in {@link SyncReport#getFailures()} so
 *       callers can decide to retry, dead-letter, or alert.</li>
 * </ul>
 *
 * <h3>Lifecycle</h3>
 * <pre>
 *   try (NativeStorageBridge bridge = new NativeStorageBridge(storagePath)) {
 *       SyncReport report = bridge.syncRecords(records);
 *       System.out.println(report);
 *   }
 * </pre>
 *
 * <h3>Thread safety</h3>
 * The underlying native storage is opened with {@code SQLITE_OPEN_FULLMUTEX};
 * concurrent calls from multiple Java threads are safe, but callers should
 * prefer a single bridge instance per process to avoid connection overhead.
 */
public final class NativeStorageBridge implements AutoCloseable {

    static {
        System.loadLibrary("transaction_storage_native"); // libtransaction_storage_native.so
    }

    /** Opaque pointer to the native storage context; 0 means closed. */
    private long nativeStorageHandle;

    // ── Construction ──────────────────────────────────────────────────────────

    /**
     * Opens (or creates) the native local storage at the given path.
     *
     * @param storagePath absolute file-system path to the database file
     * @throws IllegalArgumentException if {@code storagePath} is null or blank
     * @throws IllegalStateException    if the native storage cannot be opened
     */
    public NativeStorageBridge(String storagePath) {
        Objects.requireNonNull(storagePath, "storagePath must not be null");
        if (storagePath.isBlank()) {
            throw new IllegalArgumentException("storagePath must not be blank");
        }
        this.nativeStorageHandle = nativeOpenStorage(storagePath);
        if (this.nativeStorageHandle == 0L) {
            throw new IllegalStateException(
                    "Failed to open native storage at: " + storagePath);
        }
    }

    // ── Primary API ───────────────────────────────────────────────────────────

    /**
     * Synchronises a collection of {@link TransactionRecord} objects with the
     * native local storage by calling {@code commitToStorage()} individually
     * for each record.
     *
     * <p>Processing order is the natural list iteration order.  Every record
     * is attempted regardless of prior failures — no short-circuit behaviour.
     *
     * @param records non-null, non-empty list of records to commit
     *                (expected size: 25, but any positive size is accepted)
     * @return a {@link SyncReport} aggregating one {@link CommitResult} per record
     * @throws IllegalArgumentException if {@code records} is null or empty
     * @throws IllegalStateException    if this bridge has already been closed
     */
    public SyncReport syncRecords(List<TransactionRecord> records) {
        Objects.requireNonNull(records, "records must not be null");
        if (records.isEmpty()) {
            throw new IllegalArgumentException("records must not be empty");
        }
        assertOpen();

        final int total = records.size();
        List<CommitResult> results = new ArrayList<>(total);

        System.out.printf("[NativeStorageBridge] Starting sync of %d record(s)...%n", total);

        for (int i = 0; i < total; i++) {
            TransactionRecord record = records.get(i);

            if (record == null) {
                // Treat a null slot as a serialisation error without hitting native
                results.add(new CommitResult(
                        "NULL_RECORD_AT_INDEX_" + i,
                        false,
                        CommitResult.COMMIT_ERR_SERIALISE,
                        "Null TransactionRecord at list index " + i,
                        System.currentTimeMillis()));
                continue;
            }

            // ── Delegate to native commitToStorage() for this single record ───
            CommitResult result = commitToStorage(
                    nativeStorageHandle,
                    record.getTransactionId(),
                    record.getTimestampMs(),
                    record.getAmountMicros(),     // lossless scaled long
                    record.getCurrency(),
                    record.getType(),
                    record.getStatus(),
                    record.getAccountRef()
            );

            results.add(result);

            // Progress trace (remove or gate behind a flag in production)
            System.out.printf("[NativeStorageBridge] [%2d/%d] %s%n",
                    i + 1, total, result);
        }

        SyncReport report = new SyncReport(results);
        System.out.printf("[NativeStorageBridge] Sync complete: %s%n", report);
        return report;
    }

    // ── Native declarations ───────────────────────────────────────────────────

    /**
     * Opens the native storage at {@code storagePath}.
     *
     * @return opaque storage context pointer, or {@code 0} on failure
     */
    private native long nativeOpenStorage(String storagePath);

    /**
     * Commits a single transaction record to the native local storage inside
     * its own atomic {@code BEGIN … COMMIT} transaction (per-row ACID).
     *
     * <p>Each field of {@link TransactionRecord} is passed as a primitive or
     * {@code String} to avoid per-field JNI reflection calls on the hot path.
     *
     * @param storageHandle  opaque pointer returned by {@link #nativeOpenStorage}
     * @param transactionId  business-key UUID string
     * @param timestampMs    epoch milliseconds
     * @param amountMicros   monetary amount × 10⁶ (lossless long encoding)
     * @param currency       ISO-4217 currency code
     * @param type           {@link TransactionRecord} TYPE_* constant
     * @param status         {@link TransactionRecord} STATUS_* constant
     * @param accountRef     source account reference string
     * @return a {@link CommitResult} describing the per-record ACID outcome
     */
    private native CommitResult commitToStorage(
            long   storageHandle,
            String transactionId,
            long   timestampMs,
            long   amountMicros,
            String currency,
            int    type,
            int    status,
            String accountRef);

    /**
     * Closes the native storage and releases all associated resources.
     *
     * @param storageHandle opaque pointer returned by {@link #nativeOpenStorage}
     */
    private native void nativeCloseStorage(long storageHandle);

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void close() {
        if (nativeStorageHandle != 0L) {
            nativeCloseStorage(nativeStorageHandle);
            nativeStorageHandle = 0L;
            System.out.println("[NativeStorageBridge] Native storage closed.");
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void assertOpen() {
        if (nativeStorageHandle == 0L) {
            throw new IllegalStateException("NativeStorageBridge has already been closed.");
        }
    }
}