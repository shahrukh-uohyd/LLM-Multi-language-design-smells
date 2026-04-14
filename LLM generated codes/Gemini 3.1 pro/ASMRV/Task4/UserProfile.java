// A Java class with a constructor to be located and called by JNI
public class UserProfile {
    public String username;
    public int userId;
    public boolean isActive;

    // The constructor that the native method will locate
    public UserProfile(String username, int userId, boolean isActive) {
        this.username = username;
        this.userId = userId;
        this.isActive = isActive;
    }

    // A method to demonstrate the object was successfully created and populated
    public void display() {
        System.out.println("UserProfile Data:");
        System.out.println(" - Username: " + username);
        System.out.println(" - User ID:  " + userId);
        System.out.println(" - Active:   " + isActive);
    }
}