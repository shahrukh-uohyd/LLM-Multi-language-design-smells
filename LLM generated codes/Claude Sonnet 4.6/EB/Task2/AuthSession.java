/**
 * Represents an authenticated session.
 * Populated by the native layer and handed back to Java.
 */
public class AuthSession {

    private String  sessionToken;
    private String  role;            // e.g. "ADMIN", "USER", "GUEST"
    private long    expiresAt;       // Unix epoch milliseconds
    private boolean active;

    // No-arg constructor required so the native layer can instantiate it
    public AuthSession() {}

    /* ── Setters (called from C++ via JNI) ─────────────────────── */
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
    public void setRole(String role)                 { this.role         = role;         }
    public void setExpiresAt(long expiresAt)         { this.expiresAt    = expiresAt;    }
    public void setActive(boolean active)            { this.active       = active;       }

    /* ── Getters ────────────────────────────────────────────────── */
    public String  getSessionToken() { return sessionToken; }
    public String  getRole()         { return role;         }
    public long    getExpiresAt()    { return expiresAt;    }
    public boolean isActive()        { return active;       }

    @Override
    public String toString() {
        return "AuthSession{token='" + sessionToken + "', role='" + role +
               "', expiresAt=" + expiresAt + ", active=" + active + "}";
    }
}