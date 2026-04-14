/**
 * RobotBridge
 *
 * Central JNI bridge for the Autonomous Warehouse Robot.
 * Declares all 9 native methods across four functional groups
 * and loads the native shared library exactly once.
 *
 * Native library naming conventions:
 *   Linux   → libRobotBridge.so
 *   Windows → RobotBridge.dll
 *   macOS   → libRobotBridge.dylib
 *
 * To regenerate the C++ header after any signature change:
 *   javac -h . RobotBridge.java
 */
public class RobotBridge {

    // ----------------------------------------------------------------
    // Static initializer — library is loaded once for the entire JVM.
    // All four modules share this single load via constructor injection.
    // ----------------------------------------------------------------
    static {
        System.loadLibrary("RobotBridge");
    }

    // ================================================================
    // GROUP 1 — MotionController  (public — hardware safety primitives)
    // Direct wheel-drive and emergency-braking via the native motor HAL.
    // ================================================================

    /**
     * Commands the drive wheels to rotate at the requested velocities.
     *
     * <p>Positive values drive a wheel forward; negative values reverse it.
     * Setting both to zero coasts (no active braking — use
     * {@link #applyEmergencyBrake()} for an immediate hard stop).
     *
     * @param leftWheelRpm   Target speed for the left  drive wheel (RPM).
     *                       Valid range: [{@code -MAX_RPM}, {@code MAX_RPM}].
     * @param rightWheelRpm  Target speed for the right drive wheel (RPM).
     *                       Valid range: [{@code -MAX_RPM}, {@code MAX_RPM}].
     * @return               true  if the motor controller accepted the command;
     *                       false if a hardware fault prevented execution.
     */
    public native boolean rotateDriveWheels(float leftWheelRpm, float rightWheelRpm);

    /**
     * Triggers an immediate, unconditional hard stop on all drive motors.
     *
     * <p>This method bypasses the normal motion pipeline and writes
     * directly to the motor-controller's emergency register. It should
     * be called whenever a collision risk or critical fault is detected.
     *
     * @return true if the brake signal was acknowledged by the hardware;
     *         false if the safety bus could not be reached (escalate to
     *         a hardware watchdog reset in that case).
     */
    public native boolean applyEmergencyBrake();

    // ================================================================
    // GROUP 2 — VisionSystem  (package-private — vision pipeline only)
    // Barcode scanning, obstacle detection, and depth-map computation.
    // ================================================================

    /**
     * Captures a single frame from the barcode-scanner camera and
     * attempts to decode any 1-D or 2-D barcode present.
     *
     * @param cameraId   Index of the scanner camera to use (0-based).
     * @param timeoutMs  Maximum time to wait for a successful decode,
     *                   in milliseconds. Use 0 for a single-shot attempt.
     * @return           Decoded barcode string (e.g. an EAN-13 or QR
     *                   payload), or {@code null} if no barcode was found
     *                   within the timeout.
     */
    native String scanBarcode(int cameraId, int timeoutMs);

    /**
     * Analyses the depth sensor feed and returns the nearest obstacle's
     * range and bearing relative to the robot's current heading.
     *
     * @param sensorArrayId  Index identifying which sensor array to query.
     * @return               {@link ObstacleInfo} describing the closest
     *                       obstacle, or {@code null} if the sensor array
     *                       is offline or detects no obstacles within range.
     */
    native ObstacleInfo detectObstacle(int sensorArrayId);

    /**
     * Processes a stereo-camera frame pair and computes a dense
     * per-pixel depth map for the scene ahead of the robot.
     *
     * @param leftFrameRgba   Raw RGBA pixels from the left  stereo camera
     *                        (width × height × 4 bytes).
     * @param rightFrameRgba  Raw RGBA pixels from the right stereo camera
     *                        (width × height × 4 bytes).
     * @param frameWidth      Frame width  in pixels.
     * @param frameHeight     Frame height in pixels.
     * @return                Float array of per-pixel depth values in metres
     *                        (length = {@code frameWidth × frameHeight}),
     *                        or {@code null} if the computation failed.
     */
    native float[] calculateDepthMap(byte[] leftFrameRgba,
                                     byte[] rightFrameRgba,
                                     int    frameWidth,
                                     int    frameHeight);

    // ================================================================
    // GROUP 3 — DiagnosticsModule  (package-private — diagnostics only)
    // Battery and thermal telemetry from the native sensor bus.
    // ================================================================

    /**
     * Reads the instantaneous battery terminal voltage from the
     * power-management IC.
     *
     * @param cellGroupId  Index of the battery cell group to sample.
     *                     Pass 0 for the aggregate pack voltage.
     * @return             Battery voltage in Volts, or {@code -1.0f} if
     *                     the sensor is unresponsive.
     */
    native float checkBatteryVoltage(int cellGroupId);

    /**
     * Reads the current internal temperature at a specific sensor node
     * on the robot's main I²C bus.
     *
     * @param sensorNodeId  Node index of the temperature sensor to query.
     * @return              Temperature in degrees Celsius, or
     *                      {@link Float#NaN} if the sensor is offline.
     */
    native float getInternalTemperature(int sensorNodeId);

    // ================================================================
    // GROUP 4 — PathPlanner  (package-private — planning pipeline only)
    // A* route computation and occupancy-grid maintenance.
    // ================================================================

    /**
     * Runs the A* search algorithm from the robot's current position to
     * the supplied target coordinate on the active occupancy grid.
     *
     * @param targetX       Target cell column on the occupancy grid.
     * @param targetY       Target cell row    on the occupancy grid.
     * @param heuristicMode Heuristic to use during search:
     *                        {@link Heuristic#MANHATTAN},
     *                        {@link Heuristic#EUCLIDEAN}, or
     *                        {@link Heuristic#DIAGONAL}.
     * @return              {@link PlannedPath} containing the ordered list
     *                      of waypoints and estimated travel cost, or
     *                      {@code null} if no path exists or the grid
     *                      has not been initialised.
     */
    native PlannedPath planPathToCoordinate(int targetX, int targetY,
                                            int heuristicMode);

    /**
     * Merges a new sensor observation into the active A* occupancy grid,
     * marking cells as free, occupied, or unknown.
     *
     * <p>This must be called whenever the VisionSystem produces a fresh
     * depth map or obstacle report so that the planner's world model
     * stays current.
     *
     * @param cellX          Column of the cell being updated.
     * @param cellY          Row    of the cell being updated.
     * @param occupancyValue Cell occupancy in [0.0, 1.0]:
     *                         0.0 = certainly free,
     *                         0.5 = unknown,
     *                         1.0 = certainly occupied.
     * @return               true if the cell was updated and the internal
     *                       graph was invalidated as required;
     *                       false if the coordinates are out of bounds.
     */
    native boolean updateAStarMap(int cellX, int cellY, float occupancyValue);

    // ================================================================
    // Shared data types — populated by the native layer and consumed
    // by the module classes above.
    // ================================================================

    /**
     * Heuristic-mode constants for {@link #planPathToCoordinate}.
     * Integer values must match the native {@code HeuristicMode} enum
     * defined in {@code RobotBridge.h}.
     */
    public static final class Heuristic {
        private Heuristic() { /* constants only */ }

        /** Manhattan (city-block) distance — fastest on grid graphs. */
        public static final int MANHATTAN  = 0;
        /** Straight-line (Euclidean) distance — more accurate off-axis. */
        public static final int EUCLIDEAN  = 1;
        /** Diagonal (Chebyshev) distance — balances speed and accuracy. */
        public static final int DIAGONAL   = 2;
    }

    /**
     * Describes the closest obstacle detected by
     * {@link #detectObstacle(int)}.
     */
    public static class ObstacleInfo {
        /** Distance to the nearest obstacle in metres. */
        public final float  distanceMetres;
        /**
         * Bearing of the obstacle in degrees, relative to the robot's
         * current heading. 0° = dead ahead, positive = starboard (right).
         */
        public final float  bearingDegrees;
        /** Estimated obstacle width in metres (0 if unknown). */
        public final float  widthMetres;
        /** true if the obstacle is classified as a moving object. */
        public final boolean isMoving;

        public ObstacleInfo(float distanceMetres, float bearingDegrees,
                            float widthMetres,    boolean isMoving) {
            this.distanceMetres = distanceMetres;
            this.bearingDegrees = bearingDegrees;
            this.widthMetres    = widthMetres;
            this.isMoving       = isMoving;
        }

        @Override
        public String toString() {
            return String.format(
                "ObstacleInfo{dist=%.2f m, bearing=%.1f°, "
                + "width=%.2f m, moving=%b}",
                distanceMetres, bearingDegrees, widthMetres, isMoving);
        }
    }

    /**
     * A planned route returned by {@link #planPathToCoordinate}.
     */
    public static class PlannedPath {
        /**
         * Ordered sequence of waypoint grid cells:
         * [x0, y0, x1, y1, …, xN, yN].
         * Length is always even; divide by 2 to get waypoint count.
         */
        public final int[]  waypointCells;
        /** Estimated total travel cost in the planner's cost units. */
        public final float  estimatedCost;
        /** Total path length in metres. */
        public final float  lengthMetres;
        /** Number of A* nodes expanded during the search (diagnostic). */
        public final int    nodesExpanded;

        public PlannedPath(int[] waypointCells, float estimatedCost,
                           float lengthMetres,  int   nodesExpanded) {
            this.waypointCells  = waypointCells;
            this.estimatedCost  = estimatedCost;
            this.lengthMetres   = lengthMetres;
            this.nodesExpanded  = nodesExpanded;
        }

        /** @return Number of discrete waypoints in this path. */
        public int waypointCount() {
            return (waypointCells == null) ? 0 : waypointCells.length / 2;
        }

        @Override
        public String toString() {
            return String.format(
                "PlannedPath{waypoints=%d, cost=%.2f, "
                + "length=%.2f m, nodesExpanded=%d}",
                waypointCount(), estimatedCost,
                lengthMetres, nodesExpanded);
        }
    }
}