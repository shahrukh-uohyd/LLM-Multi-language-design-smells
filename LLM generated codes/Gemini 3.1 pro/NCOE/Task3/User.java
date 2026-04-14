package com.example.batch;

public class User {
    public String userId;
    public String status;
    public int loginAttempts;

    public User(String userId, String status, int loginAttempts) {
        this.userId = userId;
        this.status = status;
        this.loginAttempts = loginAttempts;
    }
}