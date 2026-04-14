/**
 * MotionController
 *
 * Governs all physical movement of the warehouse robot.
 * Translates high-level motion commands into native wheel-speed
 * and emergency-brake calls via {@link RobotBridge}.
 *
 * <p>Safety invariant: {@link #applyEmergencyBrake()} may be called
 * at any time — even while another motion command is in progress —
 * and will always take precedence.
 */
public class MotionController {

    // ----------------------------------------------------------------
    // Hardware limits enforced before crossing the JNI boundary
    // ----------------------------------------------------------------
    /** Maximum safe wheel speed accepted by the motor HAL (RPM). */
    public static final float MAX_RPM = 300.0f;

    /** Wheel-speed delta below which two commands are considered equal. */
    private static final float RPM_EPSILON = 0.01f;

    private final RobotBridge bridge;

    /** True while an emergency stop is in effect. */
    private volatile boolean emergencyStopActive = false;

    /**
     * @param bridge  Shared {@link RobotBridge} instance.
     */
    public MotionController(RobotBridge bridge) {
        if (bridge == null) {
            throw new IllegalArgumentException("RobotBridge must not be null.");
        }
        this.bridge = bridge;
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Drives both wheels at the requested RPM values.
     *
     * @param leftRpm   Left  wheel speed [-{@value #MAX_RPM}, +{@value #MAX_RPM}].
     * @param rightRpm  Right wheel speed [-{@value #MAX_RPM}, +{@value #MAX_RPM}].
     * @return          true if the motor HAL accepted the command.
     * @throws IllegalStateException    if an emergency stop is active.
     * @throws IllegalArgumentException if either RPM exceeds {@value #MAX_RPM}.
     */
    public boolean rotateDriveWheels(float leftRpm, float rightRpm) {
        if (emergencyStopActive) {
            throw new IllegalStateException(
                "[Motion] Cannot rotate wheels — emergency stop is active. "
                + "Call clearEmergencyStop() first.");
        }
        validateRpm("leftRpm",  leftRpm);
        validateRpm("rightRpm", rightRpm);

        System.out.printf("[Motion] Commanding wheels → left: %.1f RPM, "
                + "right: %.1f RPM%n", leftRpm, rightRpm);

        // ── Native call ──────────────────────────────────────────────
        boolean ok = bridge.rotateDriveWheels(leftRpm, rightRpm);
        // ────────────────────────────────────────────────────────────

        if (ok) {
            System.out.println("[Motion] ✓ Wheel command accepted by motor HAL.");
        } else {
            System.err.println("[Motion] ✗ ERROR: Motor HAL rejected wheel command.");
        }
        return ok;
    }

    /**
     * Triggers an immediate hard stop on all drive motors and latches
     * the emergency-stop flag so further motion commands are blocked
     * until {@link #clearEmergencyStop()} is explicitly called.
     *
     * @return true if the brake signal was acknowledged by hardware.
     */
    public boolean applyEmergencyBrake() {
        System.err.println("[Motion] ⚠  EMERGENCY BRAKE TRIGGERED.");

        // ── Native call ──────────────────────────────────────────────
        boolean acked = bridge.applyEmergencyBrake();
        // ────────────────────────────────────────────────────────────

        emergencyStopActive = true;   // latch regardless of ack result

        if (acked) {
            System.err.println("[Motion] ✓ Emergency brake acknowledged by hardware.");
        } else {
            System.err.println("[Motion] ✗ CRITICAL: Safety bus unreachable — "
                    + "escalate to watchdog reset!");
        }
        return acked;
    }

    /**
     * Clears the software emergency-stop latch, allowing motion commands
     * to be issued again.
     *
     * <p><b>Only call this after physically verifying that the hazard
     * has been resolved.</b>
     */
    public void clearEmergencyStop() {
        emergencyStopActive = false;
        System.out.println("[Motion] Emergency stop cleared. Motion re-enabled.");
    }

    /** @return true if an emergency stop is currently active. */
    public boolean isEmergencyStopActive() {
        return emergencyStopActive;
    }

    /**
     * Convenience: drive straight forward at a uniform speed.
     *
     * @param rpm  Forward speed in RPM [0, {@value #MAX_RPM}].
     * @return     true if the motor HAL accepted the command.
     */
    public boolean driveForward(float rpm) {
        System.out.printf("[Motion] Driving forward at %.1f RPM.%n", rpm);
        return rotateDriveWheels(rpm, rpm);
    }

    /**
     * Convenience: drive straight backward at a uniform speed.
     *
     * @param rpm  Reverse speed in RPM [0, {@value #MAX_RPM}].
     * @return     true if the motor HAL accepted the command.
     */
    public boolean driveBackward(float rpm) {
        System.out.printf("[Motion] Driving backward at %.1f RPM.%n", rpm);
        return rotateDriveWheels(-rpm, -rpm);
    }

    /**
     * Convenience: spin in place to the left (counter-clockwise).
     *
     * @param rpm  Turning speed in RPM [0, {@value #MAX_RPM}].
     * @return     true if the motor HAL accepted the command.
     */
    public boolean spinLeft(float rpm) {
        System.out.printf("[Motion] Spinning left at %.1f RPM.%n", rpm);
        return rotateDriveWheels(-rpm, rpm);
    }

    /**
     * Convenience: spin in place to the right (clockwise).
     *
     * @param rpm  Turning speed in RPM [0, {@value #MAX_RPM}].
     * @return     true if the motor HAL accepted the command.
     */
    public boolean spinRight(float rpm) {
        System.out.printf("[Motion] Spinning right at %.1f RPM.%n", rpm);
        return rotateDriveWheels(rpm, -rpm);
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    private static void validateRpm(String paramName, float rpm) {
        if (rpm < -MAX_RPM || rpm > MAX_RPM) {
            throw new IllegalArgumentException(
                paramName + " must be in [-" + MAX_RPM + ", " + MAX_RPM
                + "] RPM, got: " + rpm);
        }
    }
}