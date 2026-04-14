/**
 * Main authentication class.
 * Declares native methods and demonstrates the full auth + session flow.
 */
public class UserAuthenticator {

    static {
        System.loadLibrary("NativeAuthenticator");
    }

    // ── State ────────────────────────────────────────────────────────────────
    private AuthCredentials credentials;
    private AuthSession     session;
    private int             failedAttempts;

    public UserAuthenticator(AuthCredentials credentials) {
        this.credentials   = credentials;
        this.session       = new AuthSession();
        this.failedAttempts = 0;
    }

    /* ── Getters (called from C++ via JNI) ─────────────────────────────── */
    public AuthCredentials getCredentials()    { return credentials;    }
    public AuthSession     getSession()        { return session;        }
    public int             getFailedAttempts() { return failedAttempts; }

    /* ── Setter (called from C++ via JNI) ──────────────────────────────── */
    public void setFailedAttempts(int count)   { this.failedAttempts = count; }

    /* ── Native methods ─────────────────────────────────────────────────── */

    /**
     * Validates the credentials via native code.
     * On success, populates the embedded AuthSession.
     *
     * @return  true  if authentication succeeded
     *          false if credentials are invalid or the account is locked
     */
    public native boolean authenticate();

    /**
     * Validates a previously issued session token.
     *
     * @param token  the token string to validate
     * @return       true if the token is still active and not expired
     */
    public native boolean validateToken(String token);

    /**
     * Invalidates the current session (logout).
     */
    public native void revokeSession();

    /**
     * Returns a diagnostic report about the current authentication state.
     */
    public native String getAuthReport();

    /* ── Demo main ──────────────────────────────────────────────────────── */
    public static void main(String[] args) {

        // --- Successful authentication ---
        System.out.println("╔══════════════════════════════╗");
        System.out.println("║  Test 1: Valid credentials   ║");
        System.out.println("╚══════════════════════════════╝");

        AuthCredentials validCreds =
                new AuthCredentials("alice", "s3cr3t!", "192.168.1.10");
        UserAuthenticator auth1 = new UserAuthenticator(validCreds);

        boolean ok = auth1.authenticate();
        System.out.println("Authenticated : " + ok);
        System.out.println("Session       : " + auth1.getSession());

        if (ok) {
            String token = auth1.getSession().getSessionToken();
            System.out.println("Token valid   : " + auth1.validateToken(token));
            System.out.println("\nAuth Report:\n" + auth1.getAuthReport());
            auth1.revokeSession();
            System.out.println("\nAfter revoke  : " + auth1.getSession());
        }

        // --- Failed authentication ---
        System.out.println("\n╔══════════════════════════════╗");
        System.out.println("║  Test 2: Wrong password      ║");
        System.out.println("╚══════════════════════════════╝");

        AuthCredentials badCreds =
                new AuthCredentials("alice", "wrongpass", "10.0.0.5");
        UserAuthenticator auth2 = new UserAuthenticator(badCreds);

        System.out.println("Authenticated : " + auth2.authenticate());
        System.out.println("Failed attempts: " + auth2.getFailedAttempts());
        System.out.println("\nAuth Report:\n" + auth2.getAuthReport());
    }
}