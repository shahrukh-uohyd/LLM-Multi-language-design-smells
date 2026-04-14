public class AuthContext {

    // Authentication-related state
    private String username;
    private String token;
    private int privilegeLevel;
    private boolean active;

    static {
        System.loadLibrary("auth_native");
    }

    public AuthContext(String username,
                       String token,
                       int privilegeLevel,
                       boolean active) {
        this.username = username;
        this.token = token;
        this.privilegeLevel = privilegeLevel;
        this.active = active;
    }

    /**
     * Native authentication/integration check.
     * Returns true if the native layer accepts the user.
     */
    public native boolean authenticate();

    public static void main(String[] args) {
        AuthContext ctx =
            new AuthContext("alice", "TOKEN-XYZ-123", 3, true);

        boolean result = ctx.authenticate();
        System.out.println("Authentication result (native): " + result);
    }
}
