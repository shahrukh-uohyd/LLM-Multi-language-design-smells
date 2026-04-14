package com.serializer;

import java.util.HexFormat;
import java.util.Objects;

/**
 * Immutable result returned after a native serialization call.
 *
 * <p>Carries the raw binary payload produced by the native encoder,
 * the source record, and timing metadata.
 */
public final class SerializeResult {

    private final SerialRecord source;
    private final byte[]       bytes;
    private final long         elapsedNanos;

    public SerializeResult(SerialRecord source, byte[] bytes, long elapsedNanos) {
        this.source       = Objects.requireNonNull(source);
        this.bytes        = Objects.requireNonNull(bytes).clone();
        this.elapsedNanos = elapsedNanos;
    }

    // ── Accessors ──────────────────────────────────────────────────────────────
    public SerialRecord getSource()       { return source;       }
    public byte[]       getBytes()        { return bytes.clone(); }
    public int          getByteLength()   { return bytes.length; }
    public long         getElapsedNanos() { return elapsedNanos; }

    /**
     * Returns the first {@code maxBytes} bytes of the payload as an
     * upper-case hex string (e.g. {@code "4E535231..."}).
     */
    public String hexPreview(int maxBytes) {
        int len = Math.min(maxBytes, bytes.length);
        byte[] slice = new byte[len];
        System.arraycopy(bytes, 0, slice, 0, len);
        return HexFormat.of().withUpperCase().formatHex(slice)
               + (bytes.length > maxBytes ? "…" : "");
    }

    @Override
    public String toString() {
        return String.format(
            "SerializeResult{source=%s, bytes=%d, elapsedNs=%,d, hex='%s'}",
            source.getName(), bytes.length, elapsedNanos, hexPreview(16));
    }
}