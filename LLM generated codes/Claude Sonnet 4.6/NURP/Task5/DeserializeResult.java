package com.serializer;

import java.util.Objects;

/**
 * Immutable result returned after a native deserialization call.
 *
 * <p>Carries the reconstructed {@link SerialRecord}, the number of bytes
 * consumed from the input buffer, and timing metadata.
 */
public final class DeserializeResult {

    private final SerialRecord record;
    private final int          bytesConsumed;
    private final long         elapsedNanos;

    public DeserializeResult(SerialRecord record, int bytesConsumed, long elapsedNanos) {
        this.record        = Objects.requireNonNull(record);
        this.bytesConsumed = bytesConsumed;
        this.elapsedNanos  = elapsedNanos;
    }

    // ── Accessors ──────────────────────────────────────────────────────────────
    public SerialRecord getRecord()        { return record;        }
    public int          getBytesConsumed() { return bytesConsumed; }
    public long         getElapsedNanos()  { return elapsedNanos;  }

    @Override
    public String toString() {
        return String.format(
            "DeserializeResult{record=%s, bytesConsumed=%d, elapsedNs=%,d}",
            record, bytesConsumed, elapsedNanos);
    }
}