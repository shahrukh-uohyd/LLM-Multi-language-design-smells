package com.robotics.diagnostics;

import com.robotics.bridge.NativeRobotBridge;

public class DiagnosticsModule {

    public double getBatteryVoltage() {
        return NativeRobotBridge.checkBatteryVoltage();
    }

    public double getInternalTemperature() {
        return NativeRobotBridge.getInternalTemperature();
    }

    public boolean isOverheating() {
        return getInternalTemperature() > 75.0;
    }
}
