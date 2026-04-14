package com.wearable.sync;

import com.wearable.nativebridge.NativeWearableBridge;

public class SyncService {

    public void initialize() {
        boolean success = NativeWearableBridge.initializeBluetoothStack();
        if (!success) {
            throw new IllegalStateException("Bluetooth initialization failed");
        }
    }

    public String[] discoverDevices() {
        return NativeWearableBridge.scanForDevices();
    }

    public boolean syncData(String deviceId, byte[] encryptedData) {
        return NativeWearableBridge.sendDataPacket(deviceId, encryptedData);
    }
}
