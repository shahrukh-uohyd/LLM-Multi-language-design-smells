package com.example.batch;

public class User {

    // Fields accessed from JNI (must not be private)
    public int userId;
    public String status;
    public int loginAttempts;

    public User(int userId, String status, int loginAttempts) {
        this.userId = userId;
        this.status = status;
        this.loginAttempts = loginAttempts;
    }
}
