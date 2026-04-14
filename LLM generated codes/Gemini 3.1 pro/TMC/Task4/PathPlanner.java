public class PathPlanner {

    static {
        System.loadLibrary("warehouse_robot_native");
    }

    // Native declarations for GIS and pathfinding algorithms
    private native boolean updateAStarMap(byte[] occupancyGrid);
    private native double[] planPathToCoordinate(double currentX, double currentY, double targetX, double targetY);

    /**
     * Updates the internal map with new layout data and calculates the fastest 
     * route to the target picking station.
     */
    public double[] navigateToAisle(byte[] latestWarehouseMap, double curX, double curY, double destX, double destY) {
        System.out.println("Updating navigation grid with latest warehouse state...");
        
        // 1. Sync the internal map (e.g., adding newly dropped pallets as obstacles)
        boolean mapUpdated = updateAStarMap(latestWarehouseMap);
        
        if (!mapUpdated) {
            System.err.println("Warning: Failed to update occupancy grid. Using cached map.");
        }
        
        // 2. Calculate the route (Returns an array of waypoints: [x1, y1, x2, y2, ...])
        System.out.println("Calculating optimal path to coordinates: (" + destX + ", " + destY + ")");
        double[] waypoints = planPathToCoordinate(curX, curY, destX, destY);
        
        if (waypoints != null && waypoints.length > 0) {
            System.out.println("Path acquired. Total waypoints generated: " + (waypoints.length / 2));
        } else {
            System.err.println("Error: No valid path found to destination!");
        }
        
        return waypoints;
    }
}