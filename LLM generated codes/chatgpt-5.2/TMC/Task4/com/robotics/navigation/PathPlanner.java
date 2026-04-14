package com.robotics.navigation;

import com.robotics.bridge.NativeRobotBridge;

public class PathPlanner {

    public double[][] planPath(double x, double y) {
        return NativeRobotBridge.planPathToCoordinate(x, y);
    }

    public void updateMapCell(int x, int y, boolean blocked) {
        NativeRobotBridge.updateAStarMap(x, y, blocked);
    }
}
