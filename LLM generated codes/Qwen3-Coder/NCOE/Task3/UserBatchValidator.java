// UserBatchValidator.java
import java.util.List;
import java.util.ArrayList;

public class UserBatchValidator {
    // Native method declaration
    public native boolean validateUser(User user);
    
    static {
        System.loadLibrary("validation_engine"); // Load native library
    }
    
    public void processUserBatch(List<User> users) {
        System.out.println("Processing batch of " + users.size() + " users...");
        
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            boolean isValid = validateUser(user);
            
            System.out.println("User #" + (i+1) + ": " + user + " - Valid: " + isValid);
        }
        
        System.out.println("Batch processing completed.");
    }
    
    public static void main(String[] args) {
        UserBatchValidator validator = new UserBatchValidator();
        
        // Create a list of 50 users for testing
        List<User> users = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            String status = (i % 3 == 0) ? "ACTIVE" : (i % 3 == 1) ? "INACTIVE" : "SUSPENDED";
            int loginAttempts = i % 10;
            users.add(new User(i, status, loginAttempts));
        }
        
        validator.processUserBatch(users);
    }
}