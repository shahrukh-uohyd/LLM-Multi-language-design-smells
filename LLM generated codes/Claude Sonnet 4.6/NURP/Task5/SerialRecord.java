package com.serializer;

import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable data model exchanged between Java and the native serialization
 * layer.
 *
 * <h2>Binary Wire Format (little-endian)</h2>
 * <p>The native C code encodes a {@code SerialRecord} into the following
 * fixed-layout binary structure:
 * <pre>
 * ┌──────────────────────────────────────────────────────────────────────┐
 * │ MAGIC    │ 4 bytes  │ 0x4E535231 ("NSR1") — format identifier       │
 * │ id       │ 8 bytes  │ int64  — record identifier                    │
 * │ score    │ 8 bytes  │ float64 (double) — numeric score              │
 * │ active   │ 1 byte   │ 0x00 = false, 0x01 = true                     │
 * │ name_len │ 4 bytes  │ uint32 — byte length of the UTF-8 name string │
 * │ name     │ name_len │ UTF-8 encoded name bytes (no NUL terminator)  │
 * │ tag_count│ 4 bytes  │ uint32 — number of tag strings                │
 * │ tags[]   │ variable │ for each tag: [4-byte len][UTF-8 bytes]       │
 * │ data_len │ 4 bytes  │ uint32 — byte length of the raw payload       │
 * │ data     │ data_len │ arbitrary binary payload bytes                │
 * │ CHECKSUM │ 4 bytes  │ XOR-based integrity checksum                  │
 * └──────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>All integer fields are stored in <b>little-endian</b> byte order.
 */
public final class SerialRecord {

    private final long     id;
    private final String   name;
    private final double   score;
    private final boolean  active;
    private final String[] tags;
    private final byte[]   payload;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    private SerialRecord(Builder b) {
        this.id      = b.id;
        this.name    = Objects.requireNonNull(b.name,    "name must not be null");
        this.score   = b.score;
        this.active  = b.active;
        this.tags    = b.tags.clone();
        this.payload = b.payload.clone();
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public long     getId()      { return id;           }
    public String   getName()    { return name;         }
    public double   getScore()   { return score;        }
    public boolean  isActive()   { return active;       }
    public String[] getTags()    { return tags.clone(); }
    public byte[]   getPayload() { return payload.clone(); }

    // -------------------------------------------------------------------------
    // Equality & display
    // -------------------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SerialRecord r)) return false;
        return id == r.id
            && Double.compare(score, r.score) == 0
            && active == r.active
            && name.equals(r.name)
            && Arrays.equals(tags, r.tags)
            && Arrays.equals(payload, r.payload);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, name, score, active);
        result = 31 * result + Arrays.hashCode(tags);
        result = 31 * result + Arrays.hashCode(payload);
        return result;
    }

    @Override
    public String toString() {
        return String.format(
            "SerialRecord{id=%d, name='%s', score=%.4f, active=%b, " +
            "tags=%s, payloadBytes=%d}",
            id, name, score, active, Arrays.toString(tags), payload.length);
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private long     id      = 0L;
        private String   name    = "";
        private double   score   = 0.0;
        private boolean  active  = false;
        private String[] tags    = new String[0];
        private byte[]   payload = new byte[0];

        public Builder id(long v)         { id      = v; return this; }
        public Builder name(String v)     { name    = Objects.requireNonNull(v); return this; }
        public Builder score(double v)    { score   = v; return this; }
        public Builder active(boolean v)  { active  = v; return this; }
        public Builder tags(String... v)  { tags    = v.clone(); return this; }
        public Builder payload(byte[] v)  { payload = v.clone(); return this; }

        public SerialRecord build() { return new SerialRecord(this); }
    }
}