import java.util.List;

public class DatabaseBridge {
    static {
        // Load the native C++ library (e.g., libnativestorage.so or nativestorage.dll)
        System.loadLibrary("nativestorage");
    }

    /**
     * Synchronizes a collection of TransactionRecord objects to the native database.
     * Each record is committed individually to ensure row-level ACID compliance.
     *
     * @param records A list of TransactionRecord objects (expected size ~25).
     * @return true if all records were committed successfully, false if any failed.
     */
    public boolean syncTransactions(List<TransactionRecord> records) {
        if (records == null || records.isEmpty()) {
            return true;
        }

        // Iterate through the 25 records and commit them one by one
        for (TransactionRecord record : records) {
            boolean success = commitToStorage(record);
            
            if (!success) {
                // If strict row-level ACID is required, you might want to throw an exception 
                // here or implement a retry mechanism for the failed transaction.
                System.err.println("Failed to commit transaction: " + record.transactionId);
                return false; 
            }
        }
        
        return true;
    }

    /**
     * Native declaration: Commits a single TransactionRecord to the native storage.
     * 
     * @param record The transaction to commit.
     * @return true if the commit was successful.
     */
    private native boolean commitToStorage(TransactionRecord record);
}