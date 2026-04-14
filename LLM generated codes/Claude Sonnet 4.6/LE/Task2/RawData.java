/**
 * Immutable carrier for raw, low-level input data entering the pipeline.
 *
 * {@code format} names the low-level format (e.g. "HEX", "BASE64", "BINARY").
 * {@code payload} holds the actual bytes to be processed.
 */
public final class RawData {

    private final String format;
    private final byte[] payload;

    /**
     * @param format  non-null, non-empty format identifier
     * @param payload non-null byte array of raw data
     * @throws IllegalArgumentException on null / empty arguments
     */
    public RawData(String format, byte[] payload) {
        if (format == null || format.isEmpty())
            throw new IllegalArgumentException("format must not be null or empty");
        if (payload == null)
            throw new IllegalArgumentException("payload must not be null");

        this.format  = format;
        this.payload = payload.clone(); // defensive copy
    }

    /** @return the format identifier (e.g. {@code "HEX"}) */
    public String getFormat() { return format; }

    /** @return a defensive copy of the raw payload bytes */
    public byte[] getPayload() { return payload.clone(); }

    @Override
    public String toString() {
        return "RawData{format='" + format + "', payloadBytes=" + payload.length + "}";
    }
}