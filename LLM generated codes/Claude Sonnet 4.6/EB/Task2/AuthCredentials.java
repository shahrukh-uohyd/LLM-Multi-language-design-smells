/**
 * Holds the raw credentials supplied by the user during a login attempt.
 */
public class AuthCredentials {

    private final String username;
    private final String password;   // In production: store a char[] and clear after use
    private final String clientIp;

    public AuthCredentials(String username, String password, String clientIp) {
        this.username = username;
        this.password = password;
        this.clientIp = clientIp;
    }

    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getClientIp() { return clientIp; }
}