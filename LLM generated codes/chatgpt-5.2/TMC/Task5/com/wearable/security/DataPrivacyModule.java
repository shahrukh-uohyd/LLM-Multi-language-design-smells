package com.wearable.security;

import com.wearable.nativebridge.NativeWearableBridge;

public class DataPrivacyModule {

    public byte[] createSessionKey() {
        return NativeWearableBridge.generateSecureKey();
    }

    public byte[] encrypt(byte[] healthData, byte[] key) {
        if (healthData == null || key == null) {
            throw new IllegalArgumentException("Data and key must not be null");
        }
        return NativeWearableBridge.encryptHealthData(healthData, key);
    }
}
