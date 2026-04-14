/**
 * NavigationModule
 *
 * Delegates route calculation and map tile rendering to the native
 * mapping engine via InfotainmentBridge.
 */
public class NavigationModule {

    /** Routing mode constants — match the native engine's enumeration. */
    public static final int ROUTE_MODE_FASTEST  = 0;
    public static final int ROUTE_MODE_SHORTEST = 1;
    public static final int ROUTE_MODE_ECO      = 2;

    /** Supported tile image formats. */
    public static final String TILE_FORMAT_PNG  = "PNG";
    public static final String TILE_FORMAT_WEBP = "WEBP";

    private final InfotainmentBridge bridge;

    /**
     * @param bridge  Shared InfotainmentBridge instance.
     */
    public NavigationModule(InfotainmentBridge bridge) {
        if (bridge == null) {
            throw new IllegalArgumentException("InfotainmentBridge must not be null.");
        }
        this.bridge = bridge;
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Calculates the optimal route between two geo-coordinates.
     *
     * @param originLat       Origin latitude.
     * @param originLon       Origin longitude.
     * @param destinationLat  Destination latitude.
     * @param destinationLon  Destination longitude.
     * @param routeMode       One of {@link #ROUTE_MODE_FASTEST},
     *                        {@link #ROUTE_MODE_SHORTEST}, or
     *                        {@link #ROUTE_MODE_ECO}.
     * @return                Route JSON string, or null if no route found.
     */
    public String calculateRoute(double originLat,      double originLon,
                                 double destinationLat, double destinationLon,
                                 int    routeMode) {
        validateCoordinate(originLat,      "originLat",      -90,   90);
        validateCoordinate(originLon,      "originLon",      -180, 180);
        validateCoordinate(destinationLat, "destinationLat", -90,   90);
        validateCoordinate(destinationLon, "destinationLon", -180, 180);

        String modeLabel = routeModeLabel(routeMode);
        System.out.printf("[Nav] Calculating %s route: (%.5f, %.5f) → (%.5f, %.5f)...%n",
                modeLabel, originLat, originLon, destinationLat, destinationLon);

        // ── Native call ──────────────────────────────────────────────
        String routeJson = bridge.calculateRoute(
                originLat, originLon,
                destinationLat, destinationLon,
                routeMode);
        // ────────────────────────────────────────────────────────────

        if (routeJson == null) {
            System.err.println("[Nav] ERROR: No route found.");
        } else {
            System.out.println("[Nav] Route calculated. JSON length: "
                    + routeJson.length() + " chars.");
            System.out.println("[Nav] Route data: " + routeJson);
        }
        return routeJson;
    }

    /**
     * Renders a map tile and returns compressed image bytes.
     *
     * @param tileX      Tile column.
     * @param tileY      Tile row.
     * @param zoomLevel  Zoom level [0 – 20].
     * @param format     {@link #TILE_FORMAT_PNG} or {@link #TILE_FORMAT_WEBP}.
     * @return           Compressed image bytes, or null if unavailable.
     */
    public byte[] renderMapTile(int tileX, int tileY,
                                int zoomLevel, String format) {
        if (zoomLevel < 0 || zoomLevel > 20) {
            throw new IllegalArgumentException(
                    "Zoom level must be in [0, 20], got: " + zoomLevel);
        }
        if (!TILE_FORMAT_PNG.equals(format) && !TILE_FORMAT_WEBP.equals(format)) {
            throw new IllegalArgumentException(
                    "Unsupported tile format: '" + format
                    + "'. Use TILE_FORMAT_PNG or TILE_FORMAT_WEBP.");
        }

        System.out.printf("[Nav] Rendering %s tile (%d, %d) at zoom %d...%n",
                format, tileX, tileY, zoomLevel);

        // ── Native call ───────────────────────────────────────���──────
        byte[] tileBytes = bridge.renderMapTile(tileX, tileY, zoomLevel, format);
        // ────────────────────────────────────────────────────────────

        if (tileBytes == null) {
            System.err.println("[Nav] ERROR: Tile not available in cache.");
        } else {
            System.out.printf("[Nav] Tile rendered. Size: %d bytes.%n", tileBytes.length);
        }
        return tileBytes;
    }

    /**
     * Convenience: calculate a fastest route and pre-fetch the
     * origin map tile at the given zoom level.
     *
     * @param originLat       Origin latitude.
     * @param originLon       Origin longitude.
     * @param destinationLat  Destination latitude.
     * @param destinationLon  Destination longitude.
     * @param tileX           Origin tile X for pre-fetch.
     * @param tileY           Origin tile Y for pre-fetch.
     * @param zoomLevel       Zoom level for the pre-fetched tile.
     */
    public void startNavigationSession(double originLat,      double originLon,
                                       double destinationLat, double destinationLon,
                                       int    tileX,          int    tileY,
                                       int    zoomLevel) {
        System.out.println("[Nav] === Starting Navigation Session ===");
        calculateRoute(originLat, originLon,
                       destinationLat, destinationLon,
                       ROUTE_MODE_FASTEST);
        renderMapTile(tileX, tileY, zoomLevel, TILE_FORMAT_PNG);
        System.out.println("[Nav] === Session Initialised ===");
    }

    // ----------------------------------------------------------------
    // Internal helpers
    // ----------------------------------------------------------------

    private static void validateCoordinate(double value, String name,
                                           double min,   double max) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    name + " must be in [" + min + ", " + max
                    + "], got: " + value);
        }
    }

    private static String routeModeLabel(int mode) {
        switch (mode) {
            case ROUTE_MODE_FASTEST:  return "FASTEST";
            case ROUTE_MODE_SHORTEST: return "SHORTEST";
            case ROUTE_MODE_ECO:      return "ECO";
            default:                  return "UNKNOWN(" + mode + ")";
        }
    }
}