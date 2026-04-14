package com.iotcontroller.hardware;

import com.iotcontroller.nativebridge.NativeController;

public class HardwareUnit {

    public void initialize() {
        boolean ok = NativeController.performSelfTest();
        if (!ok) {
            throw new IllegalStateException("Hardware self-test failed");
        }
    }

    public void start() {
        NativeController.startMotor();
    }
}
