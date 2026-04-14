import java.util.Date;

public class UserAuthenticator {
    private String username;
    private String passwordHash; // In practice, use proper hashing
    private Date lastLogin;
    private boolean isAuthenticated;
    private int loginAttempts;
    private String role;

    // Load the native authentication library
    static {
        System.loadLibrary("auth_native");
    }

    public UserAuthenticator(String username, String password) {
        this.username = username;
        this.passwordHash = hashPassword(password); // Simple hash for demo
        this.lastLogin = null;
        this.isAuthenticated = false;
        this.loginAttempts = 0;
        this.role = "USER"; // Default role
    }

    // Native method declarations
    public native boolean authenticateWithNative(String inputPassword);
    public native String encryptCredentials(String credentials);
    public native boolean validateUserSecurity();
    public native String generateSessionToken();
    public native void logAuthenticationEvent(String eventType);
    public native boolean checkPasswordStrength(String password);

    // Utility method for simple password hashing (use proper hashing in production)
    private String hashPassword(String password) {
        return Integer.toHexString(password.hashCode());
    }

    // Getters and setters
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public Date getLastLogin() { return lastLogin; }
    public void setLastLogin(Date lastLogin) { this.lastLogin = lastLogin; }
    public boolean isAuthenticated() { return isAuthenticated; }
    public void setAuthenticated(boolean authenticated) { this.isAuthenticated = authenticated; }
    public int getLoginAttempts() { return loginAttempts; }
    public void incrementLoginAttempts() { this.loginAttempts++; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    // Public methods for authentication workflow
    public boolean authenticate(String inputPassword) {
        if (authenticateWithNative(inputPassword)) {
            this.isAuthenticated = true;
            this.lastLogin = new Date();
            this.loginAttempts = 0;
            logAuthenticationEvent("SUCCESS");
            return true;
        } else {
            this.loginAttempts++;
            logAuthenticationEvent("FAILED");
            return false;
        }
    }

    public boolean setPassword(String oldPassword, String newPassword) {
        if (!checkPasswordStrength(newPassword)) {
            return false; // Password doesn't meet strength requirements
        }
        
        // Validate old password first
        if (authenticateWithNative(oldPassword)) {
            this.passwordHash = hashPassword(newPassword);
            logAuthenticationEvent("PASSWORD_CHANGE");
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return String.format("UserAuthenticator{username='%s', authenticated=%s, loginAttempts=%d, role='%s'}",
                username, isAuthenticated, loginAttempts, role);
    }
}