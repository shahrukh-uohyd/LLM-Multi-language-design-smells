public class AuthManager {
    public static void main(String[] args) {
        try {
            // Create authenticator instance
            UserAuthenticator auth = new UserAuthenticator("john_doe", "securePassword123");
            System.out.println("Created authenticator: " + auth);

            // Test authentication
            boolean result = auth.authenticate("wrongPassword");
            System.out.println("Authentication attempt with wrong password: " + result);
            System.out.println("After failed attempt: " + auth);

            // Correct password authentication
            result = auth.authenticate("securePassword123");
            System.out.println("Authentication with correct password: " + result);
            System.out.println("After successful authentication: " + auth);

            // Test native methods directly
            String encrypted = auth.encryptCredentials("sensitive_data_123");
            System.out.println("Encrypted credentials: " + encrypted);

            boolean securityValid = auth.validateUserSecurity();
            System.out.println("Security validation: " + securityValid);

            String sessionToken = auth.generateSessionToken();
            System.out.println("Generated session token: " + sessionToken);

            // Test password change
            boolean passwordChanged = auth.setPassword("securePassword123", "newSecurePassword456");
            System.out.println("Password changed successfully: " + passwordChanged);

            // Test password strength check
            boolean strongPassword = auth.checkPasswordStrength("weak");
            System.out.println("Weak password strength check: " + strongPassword);

            strongPassword = auth.checkPasswordStrength("StrongPass123!");
            System.out.println("Strong password strength check: " + strongPassword);

        } catch (Exception e) {
            System.err.println("Authentication error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}