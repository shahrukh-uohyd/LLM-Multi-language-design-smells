public class MotionController {

    static {
        System.loadLibrary("warehouse_robot_native");
    }

    // Native declarations for motor and brake control
    private native void rotateDriveWheels(double leftVelocity, double rightVelocity);
    private native boolean applyEmergencyBrake();

    /**
     * Commands the robot to move, or immediately halts it if a critical safety constraint is violated.
     */
    public void executeMovement(double speed, double turnRate, boolean obstacleImminent) {
        if (obstacleImminent) {
            System.err.println("CRITICAL: Obstacle in movement path! Engaging emergency brakes.");
            boolean brakeEngaged = applyEmergencyBrake();
            if (!brakeEngaged) {
                System.err.println("FATAL: Brake failure detected!");
            }
            return;
        }

        System.out.println("Path clear. Adjusting wheel velocities for navigation.");
        // Calculate differential drive speeds based on desired speed and turn rate
        double leftSpeed = speed + turnRate;
        double rightSpeed = speed - turnRate;
        
        // Send hardware commands via JNI
        rotateDriveWheels(leftSpeed, rightSpeed);
    }
}