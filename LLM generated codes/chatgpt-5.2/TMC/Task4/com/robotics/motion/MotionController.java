package com.robotics.motion;

import com.robotics.bridge.NativeRobotBridge;

public class MotionController {

    public void rotate(double leftSpeed, double rightSpeed) {
        NativeRobotBridge.rotateDriveWheels(leftSpeed, rightSpeed);
    }

    public void emergencyStop() {
        NativeRobotBridge.applyEmergencyBrake();
    }
}
