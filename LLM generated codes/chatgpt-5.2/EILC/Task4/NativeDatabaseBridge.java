import java.util.List;
import java.util.Objects;

public final class NativeDatabaseBridge {

    static {
        System.loadLibrary("native_db"); // libnative_db.so / native_db.dll
    }

    /**
     * Commits a single transaction record to native storage.
     * Must be ACID-compliant at row level on the native side.
     *
     * @param id       unique transaction id
     * @param payload  serialized transaction data
     * @return true if commit succeeded, false otherwise
     */
    private static native boolean commitToStorage(long id, byte[] payload);

    /**
     * Processes and commits exactly 25 records individually.
     */
    public static void syncTransactions(List<TransactionRecord> records) {
        Objects.requireNonNull(records, "records must not be null");

        if (records.size() != 25) {
            throw new IllegalArgumentException(
                "Expected exactly 25 TransactionRecord objects"
            );
        }

        for (TransactionRecord record : records) {
            Objects.requireNonNull(record, "TransactionRecord must not be null");

            boolean committed = commitToStorage(
                record.getId(),
                record.getPayload()
            );

            if (!committed) {
                // Fail fast to preserve deterministic state
                throw new RuntimeException(
                    "Native commit failed for transaction id=" + record.getId()
                );
            }
        }
    }
}
