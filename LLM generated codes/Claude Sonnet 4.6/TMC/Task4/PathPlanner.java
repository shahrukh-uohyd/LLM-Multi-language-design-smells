/**
 * PathPlanner
 *
 * Plans collision-free routes across the warehouse occupancy grid
 * using the native A* engine via {@link RobotBridge}, and keeps the
 * grid current by feeding it new sensor observations.
 */
public class PathPlanner {

    // Occupancy value constants (match native grid encoding)
    public static final float CELL_FREE     = 0.0f;
    public static final float CELL_UNKNOWN  = 0.5f;
    public static final float CELL_OCCUPIED = 1.0f;

    private final RobotBridge bridge;

    /**
     * @param bridge  Shared {@link RobotBridge} instance.
     */
    public PathPlanner(RobotBridge bridge) {
        if (bridge == null) {
            throw new IllegalArgumentException("RobotBridge must not be null.");
        }
        this.bridge = bridge;
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Plans an A* path to the target grid coordinate.
     *
     * @param targetX       Target cell column.
     * @param targetY       Target cell row.
     * @param heuristicMode One of {@link RobotBridge.Heuristic} constants.
     * @return              {@link RobotBridge.PlannedPath} on success,
     *                      or {@code null} if no path exists.
     */
    public RobotBridge.PlannedPath planPathToCoordinate(int targetX, int targetY,
                                                        int heuristicMode) {
        if (targetX < 0 || targetY < 0) {
            throw new IllegalArgumentException(
                "Target coordinates must be non-negative, got: ("
                + targetX + ", " + targetY + ")");
        }
        validateHeuristic(heuristicMode);

        System.out.printf("[PathPlanner] Planning path to (%d, %d) "
                + "with heuristic %s...%n",
                targetX, targetY, heuristicLabel(heuristicMode));

        // ── Native call ──────────────────────────────────────────────
        RobotBridge.PlannedPath path =
                bridge.planPathToCoordinate(targetX, targetY, heuristicMode);
        // ────────────────────────────────────────────────────────────

        if (path == null) {
            System.err.printf("[PathPlanner] No path found to (%d, %d). "
                    + "Target may be blocked or grid uninitialised.%n",
                    targetX, targetY);
        } else {
            System.out.printf("[PathPlanner] ✓ Path found: %s%n", path);
            printWaypoints(path);
        }
        return path;
    }

    /**
     * Updates a single cell in the A* occupancy grid.
     *
     * @param cellX          Cell column to update.
     * @param cellY          Cell row    to update.
     * @param occupancyValue New occupancy in [0.0, 1.0]:
     *                         {@link #CELL_FREE},
     *                         {@link #CELL_UNKNOWN}, or
     *                         {@link #CELL_OCCUPIED}.
     * @return               true if the cell was successfully updated.
     */
    public boolean updateAStarMap(int cellX, int cellY, float occupancyValue) {
        if (cellX < 0 || cellY < 0) {
            throw new IllegalArgumentException(
                "Cell coordinates must be non-negative, got: ("
                + cellX + ", " + cellY + ")");
        }
        if (occupancyValue < 0.0f || occupancyValue > 1.0f) {
            throw new IllegalArgumentException(
                "Occupancy value must be in [0.0, 1.0], got: "
                + occupancyValue);
        }

        System.out.printf("[PathPlanner] Updating map cell (%d, %d) → "
                + "occupancy=%.2f (%s)%n",
                cellX, cellY, occupancyValue,
                occupancyLabel(occupancyValue));

        // ── Native call ──────────────────────────────────────────────
        boolean ok = bridge.updateAStarMap(cellX, cellY, occupancyValue);
        // ────────────────────────────────────────────────────────────

        if (ok) {
            System.out.printf("[PathPlanner] ✓ Cell (%d, %d) updated.%n",
                    cellX, cellY);
        } else {
            System.err.printf("[PathPlanner] ✗ Cell (%d, %d) out of grid bounds.%n",
                    cellX, cellY);
        }
        return ok;
    }

    /**
     * Convenience: ingest a full depth map from the VisionSystem,
     * convert each pixel to an occupancy value, and bulk-update the grid.
     *
     * <p>A pixel depth ≤ {@code obstacleThresholdM} marks the cell
     * {@link #CELL_OCCUPIED}; otherwise {@link #CELL_FREE}.
     *
     * @param depthMap            Float array of per-pixel depths (metres).
     * @param mapWidth            Occupancy-grid columns (must equal frame width).
     * @param mapHeight           Occupancy-grid rows    (must equal frame height).
     * @param obstacleThresholdM  Depth ≤ this value (m) → cell is occupied.
     * @return                    Number of cells successfully updated.
     */
    public int ingestDepthMap(float[] depthMap, int mapWidth, int mapHeight,
                              float obstacleThresholdM) {
        if (depthMap == null || depthMap.length != mapWidth * mapHeight) {
            throw new IllegalArgumentException(
                "Depth map length must equal mapWidth × mapHeight.");
        }

        System.out.printf("[PathPlanner] Ingesting %dx%d depth map "
                + "(obstacle threshold=%.2f m)...%n",
                mapWidth, mapHeight, obstacleThresholdM);

        int updated = 0;
        for (int y = 0; y < mapHeight; y++) {
            for (int x = 0; x < mapWidth; x++) {
                float depth = depthMap[y * mapWidth + x];
                float occupancy = (depth <= obstacleThresholdM)
                        ? CELL_OCCUPIED : CELL_FREE;
                if (updateAStarMap(x, y, occupancy)) updated++;
            }
        }

        System.out.printf("[PathPlanner] ✓ Depth map ingested. "
                + "%d / %d cells updated.%n", updated, depthMap.length);
        return updated;
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    private static void validateHeuristic(int mode) {
        if (mode != RobotBridge.Heuristic.MANHATTAN
                && mode != RobotBridge.Heuristic.EUCLIDEAN
                && mode != RobotBridge.Heuristic.DIAGONAL) {
            throw new IllegalArgumentException(
                "Unknown heuristic mode: " + mode
                + ". Use a RobotBridge.Heuristic constant.");
        }
    }

    private static String heuristicLabel(int mode) {
        switch (mode) {
            case RobotBridge.Heuristic.MANHATTAN:  return "MANHATTAN";
            case RobotBridge.Heuristic.EUCLIDEAN:  return "EUCLIDEAN";
            case RobotBridge.Heuristic.DIAGONAL:   return "DIAGONAL";
            default:                               return "UNKNOWN";
        }
    }

    private static String occupancyLabel(float v) {
        if (v <= CELL_FREE)     return "FREE";
        if (v >= CELL_OCCUPIED) return "OCCUPIED";
        return "UNKNOWN";
    }

    private static void printWaypoints(RobotBridge.PlannedPath path) {
        if (path.waypointCells == null) return;
        StringBuilder sb = new StringBuilder("[PathPlanner] Waypoints: ");
        for (int i = 0; i < path.waypointCells.length - 1; i += 2) {
            sb.append("(").append(path.waypointCells[i])
              .append(",").append(path.waypointCells[i + 1]).append(")");
            if (i + 2 < path.waypointCells.length) sb.append(" → ");
        }
        System.out.println(sb);
    }
}