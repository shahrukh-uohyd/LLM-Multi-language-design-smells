package com.robotics.bridge;

public final class NativeRobotBridge {

    static {
        System.loadLibrary("robot_native"); // librobot_native.so / robot_native.dll
    }

    /* ===================== Motion Control ===================== */

    // Rotates drive wheels by given angular velocity (rad/s)
    public static native void rotateDriveWheels(double leftWheelRadPerSec,
                                                 double rightWheelRadPerSec);

    // Immediately stops all motion (hardware-level)
    public static native void applyEmergencyBrake();

    /* ===================== Vision System ===================== */

    // Returns decoded barcode text or null if none detected
    public static native String scanBarcode();

    // Returns true if obstacle detected within safety threshold
    public static native boolean detectObstacle();

    // Returns a 2D depth map (meters); [row][column]
    public static native float[][] calculateDepthMap();

    /* ===================== Diagnostics ===================== */

    // Returns battery voltage in volts
    public static native double checkBatteryVoltage();

    // Returns internal temperature in Celsius
    public static native double getInternalTemperature();

    /* ===================== Path Planning ===================== */

    // Plans path and returns waypoints as [][x,y]
    public static native double[][] planPathToCoordinate(double targetX,
                                                          double targetY);

    // Updates the A* grid map with new obstacle data
    public static native void updateAStarMap(int gridX,
                                             int gridY,
                                             boolean isBlocked);
}
