/**
 * Thrown when any stage of the authentication sweep fails.
 *
 * Each instance carries the {@link Stage} that faulted, allowing
 * callers to respond precisely (e.g. log a vault-transmission failure
 * differently from a minutiae-extraction failure) and to avoid
 * leaking sensitive diagnostic details to generic catch blocks.
 *
 * <h3>Security note</h3>
 * Exception messages deliberately avoid embedding raw biometric data
 * or cryptographic material.  Only stage identifiers and generic
 * fault descriptions are exposed.
 */
public final class AuthenticationException extends Exception {

    /**
     * Identifies the pipeline stage that originated the fault.
     */
    public enum Stage {
        /** Biometric minutiae could not be extracted from the raw buffer. */
        MINUTIAE_EXTRACTION,

        /** Cryptographic signature could not be generated for the features. */
        SIGNATURE_GENERATION,

        /** Signature could not be transmitted to the secure hardware vault. */
        VAULT_TRANSMISSION
    }

    private static final long serialVersionUID = 1L;

    private final Stage stage;

    /**
     * @param stage   the stage that faulted (must not be {@code null})
     * @param message concise, non-sensitive description
     */
    public AuthenticationException(Stage stage, String message) {
        super(formatMessage(stage, message));
        this.stage = stage;
    }

    /**
     * @param stage   the stage that faulted (must not be {@code null})
     * @param message concise, non-sensitive description
     * @param cause   the underlying cause
     */
    public AuthenticationException(Stage stage, String message, Throwable cause) {
        super(formatMessage(stage, message), cause);
        this.stage = stage;
    }

    /** @return the pipeline stage that originated this exception */
    public Stage getStage() {
        return stage;
    }

    private static String formatMessage(Stage stage, String message) {
        return "[" + (stage != null ? stage.name() : "UNKNOWN_STAGE") + "] " + message;
    }
}