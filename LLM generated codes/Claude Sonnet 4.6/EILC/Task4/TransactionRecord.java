package com.example.dbridge.model;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents a single financial transaction that must be persisted to the
 * native local storage with full ACID guarantees at the row level.
 *
 * <p>All monetary amounts use {@link BigDecimal} on the Java side; the JNI
 * bridge serialises them as scaled {@code long} values (amount × 10⁶) to
 * avoid floating-point precision loss across the JNI boundary.
 */
public final class TransactionRecord {

    // ── Transaction types ─────────────────────────────────────────────────────
    public static final int TYPE_DEBIT    = 0;
    public static final int TYPE_CREDIT   = 1;
    public static final int TYPE_TRANSFER = 2;
    public static final int TYPE_REVERSAL = 3;

    // ── Status codes ──────────────────────────────────────────────────────────
    public static final int STATUS_PENDING   = 0;
    public static final int STATUS_CONFIRMED = 1;
    public static final int STATUS_FAILED    = 2;

    private final String     transactionId;    // UUID / business key
    private final long       timestampMs;      // epoch milliseconds
    private final BigDecimal amount;           // monetary value (scale 6)
    private final String     currency;         // ISO-4217, e.g. "USD"
    private final int        type;             // TYPE_* constant
    private final int        status;           // STATUS_* constant
    private final String     accountRef;       // source account reference

    public TransactionRecord(String     transactionId,
                             long       timestampMs,
                             BigDecimal amount,
                             String     currency,
                             int        type,
                             int        status,
                             String     accountRef) {
        this.transactionId = Objects.requireNonNull(transactionId, "transactionId");
        this.timestampMs   = timestampMs;
        this.amount        = Objects.requireNonNull(amount, "amount");
        this.currency      = Objects.requireNonNull(currency, "currency");
        this.type          = type;
        this.status        = status;
        this.accountRef    = Objects.requireNonNull(accountRef, "accountRef");
    }

    // ── Accessors ─────────────────────────────────────────────────────────────
    public String     getTransactionId() { return transactionId; }
    public long       getTimestampMs()   { return timestampMs;   }
    public BigDecimal getAmount()        { return amount;         }
    public String     getCurrency()      { return currency;       }
    public int        getType()          { return type;           }
    public int        getStatus()        { return status;         }
    public String     getAccountRef()    { return accountRef;     }

    /**
     * Returns the amount as a scaled long (amount × 10⁶) for lossless
     * JNI transfer.
     */
    public long getAmountMicros() {
        return amount.movePointRight(6).longValueExact();
    }

    @Override
    public String toString() {
        return String.format("TransactionRecord{id='%s', amount=%s %s, type=%d, status=%d}",
                transactionId, amount.toPlainString(), currency, type, status);
    }
}