package com.robotics.vision;

import com.robotics.bridge.NativeRobotBridge;

public class VisionSystem {

    public String readBarcode() {
        return NativeRobotBridge.scanBarcode();
    }

    public boolean isObstacleDetected() {
        return NativeRobotBridge.detectObstacle();
    }

    public float[][] getDepthMap() {
        return NativeRobotBridge.calculateDepthMap();
    }
}
