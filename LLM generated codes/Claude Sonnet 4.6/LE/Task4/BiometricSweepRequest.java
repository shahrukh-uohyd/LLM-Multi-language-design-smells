import java.util.Arrays;

/**
 * Immutable, validated carrier for the raw biometric buffer that is
 * submitted to an authentication sweep.
 *
 * <h3>Security notes</h3>
 * <ul>
 *   <li>The raw buffer is defensively copied on construction and on
 *       every read to prevent callers from mutating in-flight data.</li>
 *   <li>Call {@link #destroy()} once the request is no longer needed
 *       to zero the internal buffer and minimise the window during
 *       which sensitive bytes reside in heap memory.</li>
 *   <li>{@link #toString()} intentionally omits buffer contents.</li>
 * </ul>
 */
public final class BiometricSweepRequest {

    /** Maximum accepted raw buffer size (1 MB). */
    public static final int MAX_BUFFER_BYTES = 1024 * 1024;

    /** Minimum accepted raw buffer size. */
    public static final int MIN_BUFFER_BYTES = 16;

    private final byte[]  rawBuffer;
    private final String  subjectId;
    private volatile boolean destroyed = false;

    /**
     * @param subjectId  non-null, non-blank identifier for the enrolment
     *                   subject (e.g. employee ID, device serial)
     * @param rawBuffer  non-null raw biometric byte buffer;
     *                   length must be in [{@value #MIN_BUFFER_BYTES},
     *                   {@value #MAX_BUFFER_BYTES}]
     * @throws IllegalArgumentException on validation failure
     */
    public BiometricSweepRequest(String subjectId, byte[] rawBuffer) {
        if (subjectId == null || subjectId.isBlank())
            throw new IllegalArgumentException("subjectId must not be null or blank");
        if (rawBuffer == null)
            throw new IllegalArgumentException("rawBuffer must not be null");
        if (rawBuffer.length < MIN_BUFFER_BYTES || rawBuffer.length > MAX_BUFFER_BYTES)
            throw new IllegalArgumentException(
                "rawBuffer length " + rawBuffer.length + " is outside the permitted range ["
                + MIN_BUFFER_BYTES + ", " + MAX_BUFFER_BYTES + "]");

        this.subjectId = subjectId;
        this.rawBuffer = rawBuffer.clone(); // defensive copy
    }

    /**
     * @return the subject identifier
     */
    public String getSubjectId() {
        checkNotDestroyed();
        return subjectId;
    }

    /**
     * Returns a defensive copy of the raw biometric buffer.
     *
     * @return copy of the raw bytes
     * @throws IllegalStateException if {@link #destroy()} has been called
     */
    public byte[] getRawBuffer() {
        checkNotDestroyed();
        return rawBuffer.clone();
    }

    /**
     * Securely zeroes the internal raw buffer and marks this request as
     * destroyed.  Subsequent calls to {@link #getRawBuffer()} will throw.
     *
     * <p>This method is idempotent.
     */
    public void destroy() {
        if (!destroyed) {
            Arrays.fill(rawBuffer, (byte) 0x00);
            destroyed = true;
        }
    }

    /** @return {@code true} if {@link #destroy()} has been called */
    public boolean isDestroyed() {
        return destroyed;
    }

    private void checkNotDestroyed() {
        if (destroyed)
            throw new IllegalStateException(
                "BiometricSweepRequest has been destroyed; sensitive data is no longer available");
    }

    /**
     * Returns a non-sensitive summary; raw buffer bytes are never included.
     */
    @Override
    public String toString() {
        return "BiometricSweepRequest{subjectId='" + subjectId
                + "', bufferBytes=" + rawBuffer.length
                + ", destroyed=" + destroyed + "}";
    }
}