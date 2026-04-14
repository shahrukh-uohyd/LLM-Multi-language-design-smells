import java.time.Instant;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Orchestrates the full biometric authentication sweep by invoking
 * three native C++ operations through JNI in strict sequential order:
 *
 * <ol>
 *   <li>{@link #nativeExtractMinutiae}  — extracts biometric minutiae
 *       from the raw sensor buffer.</li>
 *   <li>{@link #nativeGenerateSignature} — generates a cryptographic
 *       signature over the extracted feature set.</li>
 *   <li>{@link #nativeTransmitToVault}  — transmits the signature to
 *       the secure hardware vault and receives an acknowledgement.</li>
 * </ol>
 *
 * <h3>Security contract</h3>
 * <ul>
 *   <li>Raw biometric and signature buffers are zeroed in a
 *       {@code finally} block immediately after use.</li>
 *   <li>The {@link BiometricSweepRequest} is destroyed at the end of
 *       every sweep, whether it succeeds or fails.</li>
 *   <li>Logging never captures raw biometric or cryptographic bytes.</li>
 * </ul>
 *
 * <h3>Thread safety</h3>
 * This class itself is stateless; it is safe to call
 * {@link #performSweep} concurrently from multiple threads provided
 * the underlying native library is thread-safe.
 */
public final class AuthenticationSweep {

    private static final Logger LOG =
            Logger.getLogger(AuthenticationSweep.class.getName());

    // ------------------------------------------------------------------ //
    //  Library bootstrap                                                   //
    // ------------------------------------------------------------------ //
    static {
        /*
         * Loads:
         *   libAuthenticationSweep.so    (Linux)
         *   libAuthenticationSweep.dylib (macOS)
         *   AuthenticationSweep.dll      (Windows)
         *
         * Set -Djava.library.path=<dir> at JVM startup, or place the
         * library on LD_LIBRARY_PATH / PATH.
         */
        System.loadLibrary("AuthenticationSweep");
    }

    // ------------------------------------------------------------------ //
    //  Native method declarations                                          //
    // ------------------------------------------------------------------ //

    /**
     * Stage 1 — Extract biometric minutiae from the raw sensor buffer.
     *
     * <p>The native implementation reads {@code rawBuffer}, applies the
     * biometric extraction algorithm, and serialises the detected minutiae
     * feature set into an opaque byte array (the "minutiae IR").
     *
     * <p>The format and contents of the returned byte array are an
     * internal contract between the native stages and are never
     * inspected by Java code.
     *
     * @param rawBuffer  raw biometric sensor data; must not be {@code null}
     *                   or empty
     * @param subjectId  subject identifier for traceability inside the
     *                   native layer (e.g. for hardware-audit logs)
     * @return opaque minutiae feature byte array; never {@code null}
     * @throws AuthenticationException (stage=MINUTIAE_EXTRACTION)
     *         if extraction fails (e.g. insufficient quality, buffer corrupt)
     */
    private native byte[] nativeExtractMinutiae(byte[] rawBuffer, String subjectId)
            throws AuthenticationException;

    /**
     * Stage 2 — Generate a cryptographic signature for the extracted features.
     *
     * <p>The native implementation consumes the minutiae IR produced by
     * {@link #nativeExtractMinutiae} and generates a cryptographic
     * signature using the algorithm and key material managed by the
     * native security module.
     *
     * @param minutiaeFeatures  opaque byte array from
     *                          {@link #nativeExtractMinutiae}
     * @param subjectId         subject identifier for native-layer audit
     * @return opaque cryptographic signature byte array; never {@code null}
     * @throws AuthenticationException (stage=SIGNATURE_GENERATION)
     *         if signing fails (e.g. key unavailable, feature set malformed)
     */
    private native byte[] nativeGenerateSignature(byte[] minutiaeFeatures, String subjectId)
            throws AuthenticationException;

    /**
     * Stage 3 — Transmit the signature to the secure hardware vault.
     *
     * <p>The native implementation opens a secure channel to the vault
     * hardware, transmits the signature, and blocks until the vault
     * returns an acknowledgement.  On success it returns the vault's
     * acknowledgement token as a {@code String}; on rejection or
     * communication failure it throws.
     *
     * @param signature  opaque byte array from {@link #nativeGenerateSignature}
     * @param subjectId  subject identifier forwarded to the vault
     * @return vault acknowledgement token string; non-null, non-empty on success
     * @throws AuthenticationException (stage=VAULT_TRANSMISSION)
     *         if the vault rejects the signature or the channel fails
     */
    private native String nativeTransmitToVault(byte[] signature, String subjectId)
            throws AuthenticationException;

    // ------------------------------------------------------------------ //
    //  Public API — full authentication sweep                             //
    // ------------------------------------------------------------------ //

    /**
     * Performs a complete authentication sweep for the given request.
     *
     * <p>The three native stages are executed in strict order.  Any
     * stage failure immediately halts the pipeline and throws
     * {@link AuthenticationException} without proceeding to subsequent
     * stages, preventing partial-state security vulnerabilities.
     *
     * <p>Intermediate sensitive buffers (minutiae IR, signature bytes)
     * are zeroed in a {@code finally} block regardless of outcome.
     * The {@link BiometricSweepRequest} is always destroyed on exit.
     *
     * <pre>
     *  rawBuffer ──extract──► minutiaeIR ──sign──► signature ──transmit──► ackToken
     *                                                                          │
     *                                                                    SweepResult
     * </pre>
     *
     * @param request  fully populated sweep request; must not be
     *                 {@code null} or already destroyed
     * @return {@link SweepResult} containing outcome and audit metadata
     * @throws IllegalArgumentException  if {@code request} is {@code null}
     *                                   or already destroyed
     * @throws AuthenticationException   if any native stage fails
     */
    public SweepResult performSweep(BiometricSweepRequest request)
            throws AuthenticationException {

        Objects.requireNonNull(request, "request must not be null");
        if (request.isDestroyed())
            throw new IllegalArgumentException(
                    "request has already been destroyed; cannot reuse");

        final String subjectId = request.getSubjectId();

        LOG.log(Level.INFO,
                "Authentication sweep started for subject ''{0}''", subjectId);

        // Sensitive intermediate buffers — declared outside try so the
        // finally block can zero them even if they were partially assigned.
        byte[] minutiaeFeatures = null;
        byte[] signature        = null;

        try {
            // ── Stage 1: Extract biometric minutiae ─────────────────── //
            LOG.log(Level.FINE,
                    "Stage 1 [{0}]: extracting minutiae", subjectId);

            minutiaeFeatures = nativeExtractMinutiae(
                    request.getRawBuffer(), subjectId);

            if (minutiaeFeatures == null || minutiaeFeatures.length == 0)
                throw new AuthenticationException(
                        AuthenticationException.Stage.MINUTIAE_EXTRACTION,
                        "Native extraction returned an empty feature set");

            LOG.log(Level.FINE,
                    "Stage 1 [{0}]: extraction complete, feature bytes={1}",
                    new Object[]{subjectId, minutiaeFeatures.length});

            // ── Stage 2: Generate cryptographic signature ────────────── //
            LOG.log(Level.FINE,
                    "Stage 2 [{0}]: generating signature", subjectId);

            signature = nativeGenerateSignature(minutiaeFeatures, subjectId);

            if (signature == null || signature.length == 0)
                throw new AuthenticationException(
                        AuthenticationException.Stage.SIGNATURE_GENERATION,
                        "Native signature generation returned an empty signature");

            LOG.log(Level.FINE,
                    "Stage 2 [{0}]: signature generated, bytes={1}",
                    new Object[]{subjectId, signature.length});

            // ── Stage 3: Transmit signature to vault ─────────────────── //
            LOG.log(Level.FINE,
                    "Stage 3 [{0}]: transmitting to vault", subjectId);

            String vaultAckToken = nativeTransmitToVault(signature, subjectId);

            boolean authenticated = (vaultAckToken != null && !vaultAckToken.isBlank());

            SweepResult result = new SweepResult(
                    subjectId,
                    authenticated,
                    authenticated ? vaultAckToken : "",
                    Instant.now());

            LOG.log(Level.INFO,
                    "Authentication sweep completed for subject ''{0}'': authenticated={1}",
                    new Object[]{subjectId, authenticated});

            return result;

        } finally {
            // ── Secure cleanup — always executed ─────────────────────── //
            zeroBuffer(minutiaeFeatures);
            zeroBuffer(signature);
            request.destroy();

            LOG.log(Level.FINE,
                    "Sensitive buffers zeroed for subject ''{0}''", subjectId);
        }
    }

    // ------------------------------------------------------------------ //
    //  Private security utilities                                          //
    // ------------------------------------------------------------------ //

    /**
     * Overwrites every byte of {@code buf} with {@code 0x00}.
     * A no-op if {@code buf} is {@code null}.
     *
     * <p>Callers must ensure the reference is not published elsewhere
     * before calling this method.
     */
    private static void zeroBuffer(byte[] buf) {
        if (buf != null) {
            java.util.Arrays.fill(buf, (byte) 0x00);
        }
    }

    // ------------------------------------------------------------------ //
    //  Smoke-test entry point                                              //
    // ------------------------------------------------------------------ //

    /**
     * Minimal integration smoke test.
     * Run with: {@code java -Djava.library.path=. AuthenticationSweep}
     */
    public static void main(String[] args) {
        AuthenticationSweep sweep = new AuthenticationSweep();

        // Synthetic 64-byte "sensor" buffer for testing
        byte[] mockSensorData = new byte[64];
        for (int i = 0; i < mockSensorData.length; i++)
            mockSensorData[i] = (byte) (i & 0xFF);

        BiometricSweepRequest request =
                new BiometricSweepRequest("SUBJECT-001", mockSensorData);

        System.out.println("Request : " + request);

        try {
            SweepResult result = sweep.performSweep(request);
            System.out.println("Result  : " + result);
        } catch (AuthenticationException ex) {
            System.err.println("Sweep failed at stage " + ex.getStage()
                    + ": " + ex.getMessage());
        }
    }
}