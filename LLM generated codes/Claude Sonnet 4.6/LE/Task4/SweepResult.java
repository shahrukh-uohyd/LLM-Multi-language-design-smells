import java.time.Instant;

/**
 * Immutable record of a completed authentication sweep.
 *
 * Carries both the outcome ({@link #isAuthenticated()}) and audit
 * metadata (subject, timestamp, vault acknowledgement token) without
 * exposing raw cryptographic material.
 */
public final class SweepResult {

    private final String  subjectId;
    private final boolean authenticated;
    private final String  vaultAckToken;
    private final Instant completedAt;

    /**
     * @param subjectId      the subject identifier from the original request
     * @param authenticated  {@code true} if the vault accepted the sweep
     * @param vaultAckToken  acknowledgement token returned by the vault
     *                       (may be an empty string if unavailable)
     * @param completedAt    timestamp at which the sweep completed
     */
    public SweepResult(String  subjectId,
                       boolean authenticated,
                       String  vaultAckToken,
                       Instant completedAt) {
        if (subjectId   == null) throw new IllegalArgumentException("subjectId must not be null");
        if (vaultAckToken == null) throw new IllegalArgumentException("vaultAckToken must not be null");
        if (completedAt == null) throw new IllegalArgumentException("completedAt must not be null");

        this.subjectId      = subjectId;
        this.authenticated  = authenticated;
        this.vaultAckToken  = vaultAckToken;
        this.completedAt    = completedAt;
    }

    /** @return the subject identifier */
    public String getSubjectId()     { return subjectId; }

    /** @return {@code true} if the vault accepted and confirmed the sweep */
    public boolean isAuthenticated() { return authenticated; }

    /**
     * @return the vault's acknowledgement token, or an empty string if
     *         the vault did not provide one (e.g. on rejection)
     */
    public String getVaultAckToken() { return vaultAckToken; }

    /** @return the instant at which the sweep pipeline completed */
    public Instant getCompletedAt()  { return completedAt; }

    @Override
    public String toString() {
        return "SweepResult{"
                + "subjectId='" + subjectId + '\''
                + ", authenticated=" + authenticated
                + ", vaultAckToken='" + vaultAckToken + '\''
                + ", completedAt=" + completedAt
                + '}';
    }
}