/**
 * WarehouseRobotMain
 *
 * End-to-end demonstration of all four robot subsystems sharing a
 * single {@link RobotBridge} instance through a complete warehouse
 * pick-and-place mission cycle.
 *
 * Mission sequence
 * ────────────────
 *  1. Diagnostics  — pre-flight health check
 *  2. PathPlanner  — plan route to pick station
 *  3. VisionSystem — obstacle scan + depth map
 *  4. PathPlanner  — ingest depth map → update grid
 *  5. MotionController — drive to waypoint
 *  6. VisionSystem — scan barcode at pick station
 *  7. Diagnostics  — mid-mission thermal spot-check
 *  8. PathPlanner  — plan route back to drop station
 *  9. MotionController — emergency brake drill
 */
public class WarehouseRobotMain {

    public static void main(String[] args) {

        // Single bridge — single native library load for the whole session.
        RobotBridge bridge = new RobotBridge();

        // Instantiate all four subsystems with the shared bridge.
        MotionController motion      = new MotionController(bridge);
        VisionSystem     vision      = new VisionSystem(bridge);
        DiagnosticsModule diag       = new DiagnosticsModule(bridge);
        PathPlanner       planner    = new PathPlanner(bridge);

        banner("Warehouse Robot — Mission Start");

        // ── 1. Pre-flight health check ───────────────────────────────
        banner("Phase 1 · Pre-Flight Diagnostics");
        DiagnosticsModule.HealthReport health =
                diag.runFullHealthCheck(2 /* cell groups */, 3 /* temp nodes */);
        if (!health.isFullyHealthy()) {
            System.err.println("[Main] ✗ Robot not healthy — aborting mission.");
            return;
        }
        System.out.println("[Main] ✓ Robot healthy. Proceeding with mission.");

        // ── 2. Plan route to pick station at grid cell (42, 17) ──────
        banner("Phase 2 · Path Planning → Pick Station");
        RobotBridge.PlannedPath toPickStation =
                planner.planPathToCoordinate(42, 17,
                        RobotBridge.Heuristic.EUCLIDEAN);
        if (toPickStation == null) {
            System.err.println("[Main] ✗ No path to pick station. Aborting.");
            return;
        }

        // ── 3. Obstacle scan + depth map before moving ───────────────
        banner("Phase 3 · Vision — Obstacle & Depth Scan");
        RobotBridge.ObstacleInfo[] obstacles =
                vision.scanAllObstacleSensors(3 /* arrays */);

        // Synthesise a tiny 2×2 stereo pair for the depth-map demo
        final int FW = 2, FH = 2;
        byte[] leftFrame  = new byte[FW * FH * 4];
        byte[] rightFrame = new byte[FW * FH * 4];
        for (int i = 0; i < leftFrame.length; i++) {
            leftFrame[i]  = (byte) (i * 17);
            rightFrame[i] = (byte) (i * 13);
        }
        float[] depthMap = vision.calculateDepthMap(
                leftFrame, rightFrame, FW, FH);

        // ── 4. Feed depth map back into the A* grid ──────────────────
        banner("Phase 4 · PathPlanner — Grid Update from Depth Map");
        if (depthMap != null) {
            planner.ingestDepthMap(depthMap, FW, FH, 0.8f /* 80 cm threshold */);
        }

        // Also manually mark two known shelf cells as occupied
        planner.updateAStarMap(10, 5,  PathPlanner.CELL_OCCUPIED);
        planner.updateAStarMap(11, 5,  PathPlanner.CELL_OCCUPIED);
        planner.updateAStarMap(12, 5,  PathPlanner.CELL_FREE);

        // ── 5. Drive forward to pick station ─────────────────────────
        banner("Phase 5 · Motion — Drive to Pick Station");
        boolean moving = motion.driveForward(120.0f);
        if (!moving) {
            System.err.println("[Main] ✗ Motor HAL rejected drive command.");
        }

        // Simulate a differential turn at a corridor junction
        motion.rotateDriveWheels(80.0f, 140.0f);   // gentle right curve

        // ── 6. Scan barcode at pick station ───────────────────────────
        banner("Phase 6 · Vision — Barcode Scan at Pick Station");
        String barcode = vision.scanBarcode(0 /* camera */, 500 /* ms */);
        if (barcode != null) {
            System.out.println("[Main] Item barcode: " + barcode);
        } else {
            System.err.println("[Main] No barcode found — manual verification needed.");
        }

        // ── 7. Mid-mission thermal spot-check ─────────────────────────
        banner("Phase 7 · Mid-Mission Diagnostics");
        float driveTempC = diag.getInternalTemperature(0 /* motor driver node */);
        float battVolts  = diag.checkBatteryVoltage(0  /* aggregate pack */);

        if (!Float.isNaN(driveTempC) && driveTempC >= DiagnosticsModule.TEMP_HIGH_THRESHOLD_C) {
            System.out.println("[Main] Reducing speed due to high motor temperature.");
            motion.driveForward(60.0f);   // throttle back
        }

        // ── 8. Plan return route to drop station at (5, 3) ───────────
        banner("Phase 8 · Path Planning → Drop Station");
        RobotBridge.PlannedPath toDropStation =
                planner.planPathToCoordinate(5, 3,
                        RobotBridge.Heuristic.MANHATTAN);
        System.out.printf("[Main] Return path: %s%n", toDropStation);

        // ── 9. Emergency brake drill ──────────────────────────────────
        banner("Phase 9 · Emergency Brake Drill");
        motion.applyEmergencyBrake();

        // Verify the safety latch blocks further motion
        try {
            motion.driveForward(50.0f);
        } catch (IllegalStateException e) {
            System.out.println("[Main] ✓ Motion correctly blocked: " + e.getMessage());
        }

        // Clear the latch after the drill
        motion.clearEmergencyStop();
        System.out.println("[Main] ✓ Emergency stop cleared — robot ready.");

        banner("Mission Complete");
    }

    // ----------------------------------------------------------------
    private static void banner(String title) {
        String bar = "═".repeat(title.length() + 4);
        System.out.println("\n╔" + bar + "╗");
        System.out.println("║  " + title + "  ║");
        System.out.println("╚" + bar + "╝");
    }
}