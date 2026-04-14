package com.iotcontroller.security;

import com.iotcontroller.nativebridge.NativeController;

public class SecurityModule {

    public byte[] encrypt(byte[] payload) {
        if (payload == null || payload.length == 0) {
            throw new IllegalArgumentException("Payload cannot be empty");
        }
        return NativeController.encryptTransmission(payload);
    }

    public boolean verifyCertificate(byte[] certificateData) {
        return NativeController.verifyCertificate(certificateData);
    }

    public long checksum(byte[] payload) {
        return NativeController.calculatePayloadChecksum(payload);
    }
}
