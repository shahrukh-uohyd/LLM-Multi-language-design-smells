// DatabaseBridge.java
import java.util.List;
import java.util.ArrayList;

public class DatabaseBridge {
    static {
        System.loadLibrary("database_bridge"); // Load native library
    }
    
    // Native method declaration for individual record commit
    public native boolean commitToStorage(String transactionId, String accountId, 
                                        double amount, String currency, long timestamp, 
                                        String description, String status);
    
    // Alternative native method for batch with individual commits (for efficiency)
    public native TransactionResult[] commitRecordsIndividually(String[] transactionIds,
                                                              String[] accountIds,
                                                              double[] amounts,
                                                              String[] currencies,
                                                              long[] timestamps,
                                                              String[] descriptions,
                                                              String[] statuses);
    
    /**
     * Commits a single transaction record to native storage
     * Ensures ACID compliance at the individual record level
     */
    public TransactionResult commitSingleRecord(TransactionRecord record) {
        long startTime = System.nanoTime();
        boolean success = commitToStorage(
            record.getTransactionId(),
            record.getAccountId(),
            record.getAmount(),
            record.getCurrency(),
            record.getTimestamp(),
            record.getDescription(),
            record.getStatus()
        );
        long commitTimeMs = (System.nanoTime() - startTime) / 1_000_000;
        
        String error = success ? null : "Failed to commit record to native storage";
        return new TransactionResult(record.getTransactionId(), success, error, commitTimeMs);
    }
    
    /**
     * Synchronizes a collection of 25 TransactionRecord objects with native storage
     * Each record is committed individually to maintain ACID compliance
     */
    public List<TransactionResult> synchronizeRecords(List<TransactionRecord> records) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<TransactionResult> results = new ArrayList<>();
        
        System.out.println("Starting synchronization of " + records.size() + " records...");
        long totalStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < records.size(); i++) {
            TransactionRecord record = records.get(i);
            TransactionResult result = commitSingleRecord(record);
            results.add(result);
            
            if (!result.isSuccess()) {
                System.err.println("Failed to commit record: " + result);
            }
            
            // Progress indication
            if ((i + 1) % 5 == 0) {
                System.out.println("Processed " + (i + 1) + "/" + records.size() + " records");
            }
        }
        
        long totalTimeMs = System.currentTimeMillis() - totalStartTime;
        System.out.println("Synchronization completed in " + totalTimeMs + "ms");
        
        return results;
    }
    
    /**
     * Optimized version: commits all records individually in native code
     * This maintains ACID compliance while reducing JNI overhead
     */
    public List<TransactionResult> synchronizeRecordsOptimized(List<TransactionRecord> records) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Prepare arrays for native call
        int size = records.size();
        String[] transactionIds = new String[size];
        String[] accountIds = new String[size];
        double[] amounts = new double[size];
        String[] currencies = new String[size];
        long[] timestamps = new long[size];
        String[] descriptions = new String[size];
        String[] statuses = new String[size];
        
        for (int i = 0; i < size; i++) {
            TransactionRecord record = records.get(i);
            transactionIds[i] = record.getTransactionId();
            accountIds[i] = record.getAccountId();
            amounts[i] = record.getAmount();
            currencies[i] = record.getCurrency();
            timestamps[i] = record.getTimestamp();
            descriptions[i] = record.getDescription();
            statuses[i] = record.getStatus();
        }
        
        // Call optimized native method
        TransactionResult[] nativeResults = commitRecordsIndividually(
            transactionIds, accountIds, amounts, currencies, 
            timestamps, descriptions, statuses
        );
        
        // Convert to List
        List<TransactionResult> results = new ArrayList<>();
        for (TransactionResult result : nativeResults) {
            results.add(result);
        }
        
        return results;
    }
    
    /**
     * Verifies the synchronization by checking which records were successfully committed
     */
    public List<String> getFailedTransactions(List<TransactionResult> results) {
        List<String> failedIds = new ArrayList<>();
        for (TransactionResult result : results) {
            if (!result.isSuccess()) {
                failedIds.add(result.getTransactionId());
            }
        }
        return failedIds;
    }
    
    /**
     * Gets summary statistics for the synchronization operation
     */
    public String getSynchronizationSummary(List<TransactionResult> results) {
        int total = results.size();
        long successful = results.stream().filter(TransactionResult::isSuccess).count();
        long failed = total - successful;
        long totalTime = results.stream().mapToLong(TransactionResult::getNativeCommitTimeMs).sum();
        
        return String.format(
            "Synchronization Summary:\n" +
            "Total Records: %d\n" +
            "Successful: %d\n" +
            "Failed: %d\n" +
            "Total Time: %d ms\n" +
            "Average Time per Record: %.2f ms",
            total, successful, failed, totalTime, 
            total > 0 ? (double) totalTime / total : 0.0
        );
    }
}