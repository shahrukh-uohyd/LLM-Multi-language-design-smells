package com.example.batch;

import java.util.ArrayList;
import java.util.List;

public class UserBatchValidator {

    static {
        // Load the compiled C++ library (e.g., libbatchvalidator.so or batchvalidator.dll)
        System.loadLibrary("batchvalidator");
    }

    // Native method declaration
    public native boolean validateUser(User user);

    // Processes a batch of exactly 50 users
    public void processBatch(List<User> users) {
        if (users == null || users.size() != 50) {
            throw new IllegalArgumentException("Batch size must be exactly 50 users.");
        }

        // Iterate over the list using a for loop
        for (User user : users) {
            // Call the native C++ method for each user
            boolean isValid = validateUser(user);
            
            // Print the validation result to the console
            System.out.println("User: " + user.userId 
                + " | Valid: " + isValid 
                + " | Final Status: " + user.status);
        }
    }

    // Call site: Simulating the batch process
    public static void main(String[] args) {
        List<User> userBatch = new ArrayList<>();
        
        // Generate exactly 50 users for the batch
        for (int i = 0; i < 50; i++) {
            // Give every 10th user too many login attempts to test the validation
            int attempts = (i % 10 == 0) ? 5 : 1; 
            userBatch.add(new User("USER_" + i, "ACTIVE", attempts));
        }

        UserBatchValidator validator = new UserBatchValidator();
        validator.processBatch(userBatch);
    }
}