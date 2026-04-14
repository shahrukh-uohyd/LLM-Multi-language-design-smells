package com.example.security;

public final class ProfileSanitizerNative {

    static {
        System.loadLibrary("profile_sanitizer");
    }

    // Native method declaration
    public static native void sanitizeProfileNative(UserProfile profile);
}
