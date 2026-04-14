public class UserAuthenticator {
    // Java-side authentication data
    private String username;
    private String authToken;
    private boolean isAuthenticated;
    private int accessLevel;

    public UserAuthenticator(String username, String authToken) {
        this.username = username;
        this.authToken = authToken;
        this.isAuthenticated = false; // Default to unauthenticated
        this.accessLevel = 0;
    }

    // The native method that will process the authentication in C++
    public native void performNativeAuthentication();

    // A callback method that the C++ code will invoke
    private void logAuthAttempt(String status) {
        System.out.println("[Java Logger] Authentication status for '" + username + "': " + status);
    }

    // Getters to check the result after native execution
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public int getAccessLevel() {
        return accessLevel;
    }

    // Load the native shared library containing the C++ implementation
    static {
        // Looks for libauth_module.so (Linux), libauth_module.dylib (Mac), or auth_module.dll (Windows)
        System.loadLibrary("auth_module");
    }

    public static void main(String[] args) {
        // 1. Create an auth attempt
        System.out.println("--- Starting Authentication Process ---");
        UserAuthenticator authSession = new UserAuthenticator("admin_user", "secure_token_123");
        
        System.out.println("Java: Before native call -> Authenticated: " + authSession.isAuthenticated());

        // 2. Delegate to native C++ code
        authSession.performNativeAuthentication();

        // 3. Check the results updated by the C++ code
        System.out.println("Java: After native call  -> Authenticated: " + authSession.isAuthenticated());
        System.out.println("Java: Granted Access Level -> " + authSession.getAccessLevel());
    }
}