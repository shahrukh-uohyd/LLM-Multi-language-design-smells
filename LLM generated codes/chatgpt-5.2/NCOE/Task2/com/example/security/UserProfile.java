package com.example.security;

public class UserProfile {

    // Must be accessible to JNI (not private)
    public String bio;

    public UserProfile(String bio) {
        this.bio = bio;
    }
}
