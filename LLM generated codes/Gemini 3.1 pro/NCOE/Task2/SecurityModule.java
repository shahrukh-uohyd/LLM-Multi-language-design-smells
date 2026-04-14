package com.example.security;

public class SecurityModule {

    static {
        // Load the compiled C library (e.g., libsecurity.so, security.dll)
        System.loadLibrary("security");
    }

    // Native method declaration
    public native void sanitizeProfile(UserProfile profile);

    // Call site: Simulating user input processing
    public static void main(String[] args) {
        SecurityModule security = new SecurityModule();
        
        // Create a profile with a bio longer than 100 characters
        String longBio = "This is a very long bio that definitely exceeds the one hundred character limit we have strictly set for our user profiles to prevent database bloat and UI issues.";
        UserProfile user1 = new UserProfile(longBio);

        System.out.println("Original Bio Length: " + user1.bio.length());
        
        // Call the C JNI method to sanitize the profile
        security.sanitizeProfile(user1);
        
        System.out.println("Sanitized Bio Length: " + user1.bio.length());
        System.out.println("Sanitized Bio: " + user1.bio);
    }
}