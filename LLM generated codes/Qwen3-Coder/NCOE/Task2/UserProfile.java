// UserProfile.java
public class UserProfile {
    private String bio;
    private String username;
    private int age;
    
    public UserProfile(String bio, String username, int age) {
        this.bio = bio;
        this.username = username;
        this.age = age;
    }
    
    public String getBio() {
        return bio;
    }
    
    public void setBio(String bio) {
        this.bio = bio;
    }
    
    public String getUsername() {
        return username;
    }
    
    public int getAge() {
        return age;
    }
    
    // Native method declaration
    public native void sanitizeBioNative();
    
    static {
        System.loadLibrary("security_module"); // Load native library
    }
}