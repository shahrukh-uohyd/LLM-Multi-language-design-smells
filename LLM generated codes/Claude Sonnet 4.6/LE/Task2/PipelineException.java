/**
 * Thrown when any stage of the data transformation pipeline fails.
 *
 * The {@link Stage} enum identifies which pipeline stage originated the failure,
 * making it straightforward to handle stage-specific errors in calling code.
 */
public class PipelineException extends RuntimeException {

    /** The pipeline stage where the failure occurred. */
    public enum Stage {
        /** Low-level data reading / interpretation stage. */
        READ,
        /** Transformation-rules application stage. */
        TRANSFORM,
        /** Output generation stage. */
        GENERATE
    }

    private static final long serialVersionUID = 1L;

    private final Stage stage;

    /**
     * @param stage   the stage that failed
     * @param message human-readable description of the failure
     */
    public PipelineException(Stage stage, String message) {
        super("[" + stage + "] " + message);
        this.stage = stage;
    }

    /**
     * @param stage   the stage that failed
     * @param message human-readable description of the failure
     * @param cause   the underlying cause
     */
    public PipelineException(Stage stage, String message, Throwable cause) {
        super("[" + stage + "] " + message, cause);
        this.stage = stage;
    }

    /** @return the pipeline stage that originated this exception */
    public Stage getStage() { return stage; }
}