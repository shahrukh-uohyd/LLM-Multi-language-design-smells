// SecurityManager.java (Example usage)
public class SecurityManager {
    public static void main(String[] args) {
        // Create a user profile with a very long bio
        String longBio = createLongBio(150); // Creates a 150 character bio
        UserProfile userProfile = new UserProfile(longBio, "john_doe", 25);
        
        System.out.println("Original bio length: " + userProfile.getBio().length());
        System.out.println("Original bio: " + userProfile.getBio());
        
        // Sanitize the bio using native method
        userProfile.sanitizeBioNative();
        
        System.out.println("Sanitized bio length: " + userProfile.getBio().length());
        System.out.println("Sanitized bio: " + userProfile.getBio());
    }
    
    private static String createLongBio(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append('A');
        }
        return sb.toString();
    }
}