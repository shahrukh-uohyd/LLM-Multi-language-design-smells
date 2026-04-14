/**
 * Thrown when any stage of the text-processing operation fails.
 *
 * The {@link Stage} enum pinpoints which of the three pipeline
 * stages (PARSE, PROCESS, GENERATE) raised the error, so callers
 * can handle or report failures with precise context.
 */
public class ProcessingException extends RuntimeException {

    /** Identifies the pipeline stage that raised the failure. */
    public enum Stage {
        /** Input text could not be parsed into structured elements. */
        PARSE,
        /** Structured elements could not be processed. */
        PROCESS,
        /** Final output could not be generated. */
        GENERATE
    }

    private static final long serialVersionUID = 1L;

    private final Stage stage;

    /**
     * @param stage   the stage where the failure occurred (must not be {@code null})
     * @param message human-readable description of the failure
     */
    public ProcessingException(Stage stage, String message) {
        super(formatMessage(stage, message));
        this.stage = stage;
    }

    /**
     * @param stage   the stage where the failure occurred (must not be {@code null})
     * @param message human-readable description of the failure
     * @param cause   the underlying cause
     */
    public ProcessingException(Stage stage, String message, Throwable cause) {
        super(formatMessage(stage, message), cause);
        this.stage = stage;
    }

    /** @return the pipeline stage that originated this exception */
    public Stage getStage() {
        return stage;
    }

    private static String formatMessage(Stage stage, String message) {
        return "[" + (stage != null ? stage.name() : "UNKNOWN") + "] " + message;
    }
}