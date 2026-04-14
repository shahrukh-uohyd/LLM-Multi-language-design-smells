/**
 * Immutable record of a single {@link System#loadLibrary} attempt.
 * Used to build the aggregated diagnostic report in
 * {@link GraphicsLibraryLoadException}.
 */
public final class LoadAttempt {

    private final String  candidateName;
    private final boolean succeeded;
    private final String  errorMessage;  // null on success

    private LoadAttempt(String candidateName, boolean succeeded, String errorMessage) {
        this.candidateName = candidateName;
        this.succeeded     = succeeded;
        this.errorMessage  = errorMessage;
    }

    /** Creates a record representing a successful load. */
    public static LoadAttempt success(String candidateName) {
        return new LoadAttempt(candidateName, true, null);
    }

    /** Creates a record representing a failed load. */
    public static LoadAttempt failure(String candidateName, UnsatisfiedLinkError error) {
        return new LoadAttempt(
            candidateName,
            false,
            error.getMessage() != null ? error.getMessage() : error.toString()
        );
    }

    public String  getCandidateName() { return candidateName; }
    public boolean isSucceeded()      { return succeeded;     }
    public String  getErrorMessage()  { return errorMessage;  }

    @Override
    public String toString() {
        return succeeded
            ? "[OK ] " + candidateName
            : "[ERR] " + candidateName + " → " + errorMessage;
    }
}