package com.serializer;

import java.util.Arrays;

/**
 * JNI bridge to the {@code native_serializer} native library.
 *
 * <h2>Serialization Operations</h2>
 * <ul>
 *   <li>{@link #serialize}        — encodes a {@link SerialRecord} to binary</li>
 *   <li>{@link #deserialize}      — decodes binary back to a {@link SerialRecord}</li>
 *   <li>{@link #serializeBatch}   — encodes an array of records to a single buffer</li>
 *   <li>{@link #deserializeBatch} — decodes a batch buffer back to an array</li>
 *   <li>{@link #validateChecksum} — verifies the XOR integrity checksum</li>
 *   <li>{@link #compressPayload}  — native run-length encoding (RLE) compression</li>
 *   <li>{@link #decompressPayload}— native RLE decompression</li>
 * </ul>
 *
 * <h2>Wire Protocol</h2>
 * <p>See {@link SerialRecord} for the full binary layout documentation.
 *
 * <h2>JNI Contract</h2>
 * <p>All {@code private native} methods work with primitive Java types and
 * byte arrays — no C code ever touches Java object fields directly.
 * The public wrapper methods assemble/disassemble {@link SerialRecord}
 * objects and build the result carriers.
 */
public final class NativeSerializer implements AutoCloseable {

    static {
        NativeLibraryLoader.load();
    }

    // =========================================================================
    // Native method declarations  (implemented in native_serializer.c)
    // =========================================================================

    /**
     * Encodes a record's fields into a binary byte array.
     *
     * @param id       record identifier
     * @param name     record name (UTF-8)
     * @param score    floating-point score
     * @param active   boolean flag
     * @param tags     array of tag strings
     * @param payload  raw binary payload
     * @return         binary-encoded byte array
     */
    private native byte[] nativeSerialize(
            long id, String name, double score, boolean active,
            String[] tags, byte[] payload);

    /**
     * Decodes a binary buffer into its constituent field values.
     * Returns an {@code Object[]} in the exact order:
     * {@code [Long id, String name, Double score, Boolean active,
     *          String[] tags, byte[] payload, Integer bytesConsumed]}.
     */
    private native Object[] nativeDeserialize(byte[] data, int offset, int length);

    /**
     * Encodes multiple records into a single contiguous binary buffer.
     * The buffer begins with a 4-byte little-endian record count.
     */
    private native byte[] nativeSerializeBatch(
            long[] ids, String[] names, double[] scores, boolean[] actives,
            String[][] tags, byte[][] payloads);

    /**
     * Decodes a batch buffer produced by {@link #nativeSerializeBatch}.
     * Returns a flat {@code Object[]} containing consecutive field arrays:
     * {@code [long[] ids, String[] names, double[] scores, boolean[] actives,
     *          String[][] tags, byte[][] payloads]}.
     */
    private native Object[] nativeDeserializeBatch(byte[] data, int length);

    /**
     * Validates the trailing XOR checksum of a serialized buffer.
     *
     * @return {@code true} if the checksum is valid
     */
    private native boolean nativeValidateChecksum(byte[] data, int length);

    /**
     * Compresses a byte array using native run-length encoding (RLE).
     *
     * @return compressed byte array (may be larger for incompressible input)
     */
    private native byte[] nativeCompressPayload(byte[] data, int length);

    /**
     * Decompresses a byte array produced by {@link #nativeCompressPayload}.
     *
     * @return original uncompressed byte array
     */
    private native byte[] nativeDecompressPayload(byte[] data, int length);

    // =========================================================================
    // Public API
    // =========================================================================

    // ── Single record ─────────────────────────────────────────────────────────

    /**
     * Serializes a single {@link SerialRecord} to a binary buffer.
     *
     * @param record the record to encode (must not be null)
     * @return       a {@link SerializeResult} carrying the binary bytes and timing
     * @throws SerializerException if native encoding fails
     */
    public SerializeResult serialize(SerialRecord record) {
        requireNonNull(record, "record");
        long t0 = System.nanoTime();
        try {
            byte[] bytes = nativeSerialize(
                    record.getId(), record.getName(), record.getScore(),
                    record.isActive(), record.getTags(), record.getPayload());
            return new SerializeResult(record, bytes, System.nanoTime() - t0);
        } catch (Exception e) {
            throw new SerializerException("Serialization failed for record id=" +
                                          record.getId(), e);
        }
    }

    /**
     * Deserializes a binary buffer (from offset 0) back into a
     * {@link DeserializeResult}.
     *
     * @param data the binary buffer to decode
     * @return     a {@link DeserializeResult} with the reconstructed record
     * @throws SerializerException if native decoding fails or the magic/checksum
     *                             is invalid
     */
    public DeserializeResult deserialize(byte[] data) {
        return deserialize(data, 0, data.length);
    }

    /**
     * Deserializes a slice of a binary buffer into a {@link DeserializeResult}.
     *
     * @param data   source byte array
     * @param offset byte offset to start reading from
     * @param length number of bytes available from {@code offset}
     */
    public DeserializeResult deserialize(byte[] data, int offset, int length) {
        requireNonNull(data, "data");
        validateRange(data, offset, length);
        long t0 = System.nanoTime();
        try {
            Object[] parts = nativeDeserialize(data, offset, length);
            SerialRecord record = assembleRecord(parts);
            int consumed = (Integer) parts[6];
            return new DeserializeResult(record, consumed, System.nanoTime() - t0);
        } catch (SerializerException e) {
            throw e;
        } catch (Exception e) {
            throw new SerializerException("Deserialization failed at offset=" + offset, e);
        }
    }

    // ── Batch operations ──────────────────────────────────────────────────────

    /**
     * Serializes an array of {@link SerialRecord}s into a single contiguous
     * binary buffer suitable for bulk transfer or storage.
     *
     * @param records the records to encode (must not be null or empty)
     * @return        a {@link SerializeResult} with the batch buffer;
     *                {@code getSource()} returns the first record
     * @throws SerializerException if native batch encoding fails
     */
    public SerializeResult serializeBatch(SerialRecord[] records) {
        requireNonNull(records, "records");
        if (records.length == 0)
            throw new IllegalArgumentException("records array must not be empty");

        // Decompose object array into parallel primitive arrays for JNI.
        int n = records.length;
        long[]     ids      = new long[n];
        String[]   names    = new String[n];
        double[]   scores   = new double[n];
        boolean[]  actives  = new boolean[n];
        String[][] tags     = new String[n][];
        byte[][]   payloads = new byte[n][];

        for (int i = 0; i < n; i++) {
            SerialRecord r = Objects.requireNonNull(records[i], "records[" + i + "] is null");
            ids[i]      = r.getId();
            names[i]    = r.getName();
            scores[i]   = r.getScore();
            actives[i]  = r.isActive();
            tags[i]     = r.getTags();
            payloads[i] = r.getPayload();
        }

        long t0 = System.nanoTime();
        try {
            byte[] bytes = nativeSerializeBatch(ids, names, scores, actives, tags, payloads);
            return new SerializeResult(records[0], bytes, System.nanoTime() - t0);
        } catch (Exception e) {
            throw new SerializerException("Batch serialization failed", e);
        }
    }

    /**
     * Deserializes a batch buffer produced by {@link #serializeBatch} back
     * into an array of {@link SerialRecord}s.
     *
     * @param data the batch binary buffer
     * @return     the reconstructed {@link SerialRecord} array
     * @throws SerializerException if native batch decoding fails
     */
    public SerialRecord[] deserializeBatch(byte[] data) {
        requireNonNull(data, "data");
        long t0 = System.nanoTime();
        try {
            Object[] parts = nativeDeserializeBatch(data, data.length);
            return assembleBatch(parts);
        } catch (SerializerException e) {
            throw e;
        } catch (Exception e) {
            throw new SerializerException("Batch deserialization failed", e);
        }
    }

    // ── Integrity & compression ───────────────────────────────────────────────

    /**
     * Validates the XOR checksum embedded at the end of a serialized buffer.
     *
     * @param data the buffer to check
     * @return     {@code true} if the checksum is valid
     */
    public boolean validateChecksum(byte[] data) {
        requireNonNull(data, "data");
        return nativeValidateChecksum(data, data.length);
    }

    /**
     * Compresses a byte array using native run-length encoding.
     *
     * @param data the bytes to compress
     * @return     compressed bytes
     */
    public byte[] compressPayload(byte[] data) {
        requireNonNull(data, "data");
        return nativeCompressPayload(data, data.length);
    }

    /**
     * Decompresses a byte array previously compressed by {@link #compressPayload}.
     *
     * @param data the compressed bytes
     * @return     original uncompressed bytes
     */
    public byte[] decompressPayload(byte[] data) {
        requireNonNull(data, "data");
        return nativeDecompressPayload(data, data.length);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Reassembles a {@link SerialRecord} from the flat {@code Object[]} returned
     * by {@link #nativeDeserialize}.
     * Layout: [Long, String, Double, Boolean, String[], byte[], Integer]
     */
    private static SerialRecord assembleRecord(Object[] parts) {
        return SerialRecord.builder()
                .id     ((Long)    parts[0])
                .name   ((String)  parts[1])
                .score  ((Double)  parts[2])
                .active ((Boolean) parts[3])
                .tags   ((String[])parts[4])
                .payload((byte[])  parts[5])
                .build();
    }

    /**
     * Reassembles a {@link SerialRecord} array from the flat {@code Object[]}
     * returned by {@link #nativeDeserializeBatch}.
     * Layout: [long[], String[], double[], boolean[], String[][], byte[][]]
     */
    private static SerialRecord[] assembleBatch(Object[] parts) {
        long[]     ids      = (long[])     parts[0];
        String[]   names    = (String[])   parts[1];
        double[]   scores   = (double[])   parts[2];
        boolean[]  actives  = (boolean[])  parts[3];
        String[][] tags     = (String[][]) parts[4];
        byte[][]   payloads = (byte[][])   parts[5];

        SerialRecord[] records = new SerialRecord[ids.length];
        for (int i = 0; i < ids.length; i++) {
            records[i] = SerialRecord.builder()
                    .id     (ids[i])
                    .name   (names[i])
                    .score  (scores[i])
                    .active (actives[i])
                    .tags   (tags[i])
                    .payload(payloads[i])
                    .build();
        }
        return records;
    }

    private static void requireNonNull(Object obj, String name) {
        if (obj == null)
            throw new IllegalArgumentException(name + " must not be null");
    }

    private static void validateRange(byte[] data, int offset, int length) {
        if (offset < 0 || length < 0 || offset + length > data.length)
            throw new IllegalArgumentException(
                    "Invalid range: offset=" + offset + " length=" + length +
                    " array.length=" + data.length);
    }

    // ── AutoCloseable ─────────────────────────────────────────────────────────
    @Override public void close() { /* no native handles to release */ }

    // ── Exception ─────────────────────────────────────────────────────────────

    /** Thrown when a native serialize/deserialize operation fails. */
    public static final class SerializerException extends RuntimeException {
        public SerializerException(String msg)                 { super(msg); }
        public SerializerException(String msg, Throwable cause){ super(msg, cause); }
    }

    // Needed for the batch helper
    private static <T> T Objects_requireNonNull(T obj, String msg) {
        if (obj == null) throw new NullPointerException(msg);
        return obj;
    }
    private static final class Objects {
        static <T> T requireNonNull(T o, String m) {
            if (o == null) throw new NullPointerException(m);
            return o;
        }
    }
}