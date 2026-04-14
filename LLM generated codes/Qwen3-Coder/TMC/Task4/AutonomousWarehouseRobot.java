// AutonomousWarehouseRobot.java
public class AutonomousWarehouseRobot {
    static {
        System.loadLibrary("warehouse_robot_native");
    }

    // Motion controller methods
    private native void rotateDriveWheels(double leftSpeed, double rightSpeed);
    private native void applyEmergencyBrake();

    // Vision system methods
    private native String scanBarcode();
    private native boolean detectObstacle(double distanceThreshold);
    private native int[][] calculateDepthMap();

    // Diagnostics module methods
    private native double checkBatteryVoltage();
    private native double getInternalTemperature();

    // Path planner methods
    private native String planPathToCoordinate(double x, double y);
    private native void updateAStarMap();

    private static final AutonomousWarehouseRobot INSTANCE = new AutonomousWarehouseRobot();
    
    private AutonomousWarehouseRobot() {}
    
    public static AutonomousWarehouseRobot getInstance() {
        return INSTANCE;
    }
}

class MotionController {
    private AutonomousWarehouseRobot robot = AutonomousWarehouseRobot.getInstance();

    public void moveRobot(double leftSpeed, double rightSpeed) {
        robot.rotateDriveWheels(leftSpeed, rightSpeed);
    }

    public void stopRobot() {
        robot.applyEmergencyBrake();
    }
}

class VisionSystem {
    private AutonomousWarehouseRobot robot = AutonomousWarehouseRobot.getInstance();

    public String readProductCode() {
        return robot.scanBarcode();
    }

    public boolean isPathClear(double threshold) {
        return !robot.detectObstacle(threshold);
    }

    public int[][] getDepthInformation() {
        return robot.calculateDepthMap();
    }
}

class DiagnosticsModule {
    private AutonomousWarehouseRobot robot = AutonomousWarehouseRobot.getInstance();

    public double getBatteryStatus() {
        return robot.checkBatteryVoltage();
    }

    public double getThermalReading() {
        return robot.getInternalTemperature();
    }
}

class PathPlanner {
    private AutonomousWarehouseRobot robot = AutonomousWarehouseRobot.getInstance();

    public String navigateTo(double targetX, double targetY) {
        return robot.planPathToCoordinate(targetX, targetY);
    }

    public void refreshNavigationMap() {
        robot.updateAStarMap();
    }
}