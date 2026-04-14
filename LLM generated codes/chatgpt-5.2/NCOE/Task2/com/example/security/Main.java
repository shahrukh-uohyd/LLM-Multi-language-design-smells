package com.example.security;

public class Main {

    public static void main(String[] args) {
        UserProfile profile = new UserProfile(
            "This is a very long bio that definitely exceeds one hundred characters. "
          + "It keeps going and going until it needs to be truncated by JNI."
        );

        ProfileSanitizerNative.sanitizeProfileNative(profile);

        System.out.println("Sanitized bio:");
        System.out.println(profile.bio);
    }
}
