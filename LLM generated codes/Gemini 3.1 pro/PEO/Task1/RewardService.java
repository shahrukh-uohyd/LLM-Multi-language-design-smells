package com.example;

public class RewardService {
    
    // Load the native library (e.g., liblegacyreward.so or legacyreward.dll)
    static {
        System.loadLibrary("legacyreward");
    }

    // The native method declaration that passes the UserAccount object
    public native boolean isEligibleForLegacyReward(UserAccount account);
}