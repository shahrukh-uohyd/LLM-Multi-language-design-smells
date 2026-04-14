package com.example.batch;

import java.util.List;
import java.util.ArrayList;

public class UserBatchValidator {

    static {
        System.loadLibrary("user_validator");
    }

    // Native validation method
    public static native boolean validateUser(User user);

    public static void main(String[] args) {

        // Create exactly 50 users
        List<User> users = new ArrayList<>(50);
        for (int i = 0; i < 50; i++) {
            users.add(new User(i + 1, "PENDING", i % 5));
        }

        // Batch validation loop
        for (User user : users) {
            boolean valid = validateUser(user);
            System.out.println(
                "User " + user.userId +
                " | valid=" + valid +
                " | status=" + user.status +
                " | attempts=" + user.loginAttempts
            );
        }
    }
}
