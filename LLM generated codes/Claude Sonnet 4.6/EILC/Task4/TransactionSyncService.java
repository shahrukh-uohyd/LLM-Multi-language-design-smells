package com.example.dbridge;

import com.example.dbridge.jni.NativeStorageBridge;
import com.example.dbridge.model.CommitResult;
import com.example.dbridge.model.SyncReport;
import com.example.dbridge.model.TransactionRecord;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service entry-point demonstrating per-record ACID synchronisation
 * of 25 {@link TransactionRecord} objects via the JNI bridge.
 */
public class TransactionSyncService {

    private static final String STORAGE_PATH = "/data/local/tmp/transactions.db";
    private static final int    BATCH_SIZE   = 25;

    public static void main(String[] args) {

        List<TransactionRecord> records = buildSampleBatch(BATCH_SIZE);

        // try-with-resources guarantees nativeCloseStorage() is always called
        try (NativeStorageBridge bridge = new NativeStorageBridge(STORAGE_PATH)) {

            // ── Sync all 25 records, each committed individually (ACID) ──────
            SyncReport report = bridge.syncRecords(records);

            System.out.println("\n═══ Sync Report ═══");
            System.out.println(report);

            // ── Report failures ───────────────────────────────────────────────
            if (!report.isFullyCommitted()) {
                System.err.printf("%n⚠  %d record(s) failed to commit:%n",
                        report.getFailureCount());
                for (CommitResult failure : report.getFailures()) {
                    System.err.printf("   ✗ %s%n", failure);
                }
            } else {
                System.out.println("✓ All " + report.getTotalRecords()
                        + " records committed successfully.");
            }

        } catch (IllegalStateException e) {
            System.err.println("Fatal: could not open native storage — " + e.getMessage());
        }
    }

    // ── Build a synthetic batch of 25 TransactionRecords ─────────────────────
    private static List<TransactionRecord> buildSampleBatch(int count) {
        List<TransactionRecord> batch = new ArrayList<>(count);
        long baseTime = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            batch.add(new TransactionRecord(
                UUID.randomUUID().toString(),
                baseTime + (i * 1000L),
                new BigDecimal("100.00").add(new BigDecimal(i)),
                "USD",
                i % 2 == 0
                    ? TransactionRecord.TYPE_DEBIT
                    : TransactionRecord.TYPE_CREDIT,
                TransactionRecord.STATUS_CONFIRMED,
                "ACC-" + String.format("%05d", 1000 + i)
            ));
        }
        return batch;
    }
}