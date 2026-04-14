/**
 * Immutable carrier for the final output produced by the transformation pipeline.
 *
 * {@code outputFormat} describes the format of the generated output.
 * {@code data}         holds the transformed bytes.
 * {@code metadata}     is a human-readable summary string produced by the C++ layer.
 */
public final class PipelineResult {

    private final String outputFormat;
    private final byte[] data;
    private final String metadata;

    /**
     * @param outputFormat non-null output format label
     * @param data         non-null transformed byte array
     * @param metadata     non-null metadata / summary string
     */
    public PipelineResult(String outputFormat, byte[] data, String metadata) {
        if (outputFormat == null) throw new IllegalArgumentException("outputFormat must not be null");
        if (data         == null) throw new IllegalArgumentException("data must not be null");
        if (metadata     == null) throw new IllegalArgumentException("metadata must not be null");

        this.outputFormat = outputFormat;
        this.data         = data.clone();
        this.metadata     = metadata;
    }

    /** @return the output format label */
    public String getOutputFormat() { return outputFormat; }

    /** @return a defensive copy of the transformed data bytes */
    public byte[] getData() { return data.clone(); }

    /** @return metadata / summary string from the C++ layer */
    public String getMetadata() { return metadata; }

    @Override
    public String toString() {
        return "PipelineResult{outputFormat='" + outputFormat
                + "', dataBytes=" + data.length
                + ", metadata='" + metadata + "'}";
    }
}