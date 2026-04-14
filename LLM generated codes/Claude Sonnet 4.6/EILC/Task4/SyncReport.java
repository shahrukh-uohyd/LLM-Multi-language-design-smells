package com.example.dbridge.model;

import java.util.Collections;
import java.util.List;

/**
 * Aggregated result of synchronising the full batch of 25
 * {@link TransactionRecord} objects with the native storage.
 *
 * <p>Holds a per-record {@link CommitResult} list so callers can inspect,
 * retry, or alert on any individual failure.
 */
public final class SyncReport {

    private final int                totalRecords;
    private final int                successCount;
    private final int                failureCount;
    private final List<CommitResult> results;         // one entry per record
    private final List<CommitResult> failures;        // subset: committed == false

    public SyncReport(List<CommitResult> results) {
        this.results      = Collections.unmodifiableList(results);
        this.totalRecords = results.size();

        int successes = 0;
        for (CommitResult r : results) {
            if (r.isCommitted()) successes++;
        }

        this.successCount = successes;
        this.failureCount = totalRecords - successes;

        this.failures = results.stream()
                .filter(r -> !r.isCommitted())
                .collect(java.util.stream.Collectors.toUnmodifiableList());
    }

    // ── Accessors ─────────────────────────────────────────────────────────────
    public int                getTotalRecords()  { return totalRecords; }
    public int                getSuccessCount()  { return successCount; }
    public int                getFailureCount()  { return failureCount; }
    public List<CommitResult> getResults()       { return results;      }
    public List<CommitResult> getFailures()      { return failures;     }

    /** Returns {@code true} if every record was committed successfully. */
    public boolean isFullyCommitted() { return failureCount == 0; }

    @Override
    public String toString() {
        return String.format(
            "SyncReport{total=%d, committed=%d, failed=%d, fullyCommitted=%b}",
            totalRecords, successCount, failureCount, isFullyCommitted());
    }
}