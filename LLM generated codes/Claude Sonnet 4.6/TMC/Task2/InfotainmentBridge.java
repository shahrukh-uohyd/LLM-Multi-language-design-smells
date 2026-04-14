/**
 * InfotainmentBridge
 *
 * Central JNI bridge for the Automotive Infotainment system.
 * Declares all 8 native methods across four functional groups
 * and loads the native shared library exactly once.
 *
 * Native library naming conventions:
 *   Linux   → libInfotainmentBridge.so
 *   Windows → InfotainmentBridge.dll
 *   macOS   → libInfotainmentBridge.dylib
 *
 * To regenerate the C++ header:
 *   javac -h . InfotainmentBridge.java
 */
public class InfotainmentBridge {

    // ----------------------------------------------------------------
    // Static initializer — library is loaded once for the entire JVM
    // ----------------------------------------------------------------
    static {
        System.loadLibrary("InfotainmentBridge");
    }

    // ================================================================
    // GROUP 1 — DashboardDisplay
    // Reads live vehicle sensor data from the CAN bus via native layer.
    // ================================================================

    /**
     * Reads the current fuel level from the vehicle's fuel sensor.
     *
     * @param sensorId  Hardware sensor index (0-based).
     * @return          Fuel level as a percentage [0.0 – 100.0],
     *                  or -1.0f if the sensor is unavailable.
     */
    native float readFuelLevel(int sensorId);

    /**
     * Reads the current engine coolant temperature.
     *
     * @param sensorId  Hardware sensor index (0-based).
     * @return          Temperature in degrees Celsius,
     *                  or Float.NaN if the sensor is unavailable.
     */
    native float readEngineTemperature(int sensorId);

    // ================================================================
    // GROUP 2 — MediaCenter
    // Controls the native audio DSP and media decoder pipeline.
    // ================================================================

    /**
     * Opens and begins streaming an MP3 audio source.
     *
     * @param streamUri   URI of the MP3 resource
     *                    (file path, HTTP URL, or BT audio source).
     * @param volumeLevel Initial playback volume [0 – 100].
     * @return            A non-negative stream handle on success,
     *                    or -1 if the stream could not be opened.
     */
    native int playMp3Stream(String streamUri, int volumeLevel);

    /**
     * Adjusts the DSP equalizer bands for the active audio output.
     *
     * @param streamHandle  Handle returned by {@link #playMp3Stream}.
     * @param bands         Array of gain values in dB for each EQ band.
     *                      Typical band layout (indices):
     *                        0 = 60 Hz, 1 = 250 Hz, 2 = 1 kHz,
     *                        3 = 4 kHz, 4 = 16 kHz.
     * @return              true if the EQ was applied successfully.
     */
    native boolean adjustEqualizer(int streamHandle, float[] bands);

    // ================================================================
    // GROUP 3 — NavigationModule
    // Interfaces with the native mapping and routing engine.
    // ================================================================

    /**
     * Calculates the optimal route between two geo-coordinates.
     *
     * @param originLat       Latitude of the starting point.
     * @param originLon       Longitude of the starting point.
     * @param destinationLat  Latitude of the destination.
     * @param destinationLon  Longitude of the destination.
     * @param routeMode       Routing strategy:
     *                          0 = fastest, 1 = shortest, 2 = eco.
     * @return                Serialised route as a JSON string
     *                        (waypoints, distance, ETA),
     *                        or null if no route could be found.
     */
    native String calculateRoute(double originLat,    double originLon,
                                 double destinationLat, double destinationLon,
                                 int    routeMode);

    /**
     * Renders a single map tile as a compressed image byte array.
     *
     * @param tileX    Tile column index in the slippy-map grid.
     * @param tileY    Tile row index in the slippy-map grid.
     * @param zoomLevel  Map zoom level [0 – 20].
     * @param format   Image format token: "PNG" or "WEBP".
     * @return         Compressed image bytes ready for display,
     *                 or null if the tile is not cached / available.
     */
    native byte[] renderMapTile(int tileX, int tileY,
                                int zoomLevel, String format);

    // ================================================================
    // GROUP 4 — ConnectivitySuite
    // Manages Bluetooth device pairing and POI search.
    // ================================================================

    /**
     * Initiates a Bluetooth pairing request to a remote device.
     *
     * @param deviceMacAddress  MAC address of the target device,
     *                          e.g. "AA:BB:CC:DD:EE:FF".
     * @param pairingPin        Numeric PIN string for legacy pairing,
     *                          or an empty string for SSP pairing.
     * @return                  A {@link BluetoothPairingResult} describing
     *                          the pairing outcome.
     */
    native BluetoothPairingResult pairBluetoothDevice(String deviceMacAddress,
                                                      String pairingPin);

    /**
     * Searches for Points of Interest near a given location.
     *
     * @param latitude   Centre latitude of the search area.
     * @param longitude  Centre longitude of the search area.
     * @param radiusKm   Search radius in kilometres.
     * @param category   POI category filter, e.g. "FUEL_STATION",
     *                   "RESTAURANT", "PARKING". Pass null for all.
     * @return           Array of {@link PointOfInterest} records,
     *                   or an empty array if none are found.
     */
    native PointOfInterest[] searchPointsOfInterest(double latitude,
                                                    double longitude,
                                                    double radiusKm,
                                                    String category);

    // ================================================================
    // Shared result types — returned by native methods above
    // ================================================================

    /**
     * Immutable result record returned by {@link #pairBluetoothDevice}.
     */
    public static class BluetoothPairingResult {
        /** true if pairing completed successfully. */
        public final boolean success;
        /** Human-readable status message from the native layer. */
        public final String  statusMessage;
        /** Assigned Bluetooth profile mask (bitmask of BT profiles). */
        public final int     profileMask;

        public BluetoothPairingResult(boolean success,
                                      String  statusMessage,
                                      int     profileMask) {
            this.success       = success;
            this.statusMessage = statusMessage;
            this.profileMask   = profileMask;
        }

        @Override
        public String toString() {
            return String.format("BluetoothPairingResult{success=%b, "
                    + "status='%s', profileMask=0x%X}",
                    success, statusMessage, profileMask);
        }
    }

    /**
     * Immutable record representing a single Point of Interest.
     */
    public static class PointOfInterest {
        /** Display name of the POI. */
        public final String name;
        /** POI category (matches the category filter used in search). */
        public final String category;
        /** Latitude of the POI. */
        public final double latitude;
        /** Longitude of the POI. */
        public final double longitude;
        /** Straight-line distance from the search origin, in kilometres. */
        public final double distanceKm;

        public PointOfInterest(String name, String category,
                               double latitude, double longitude,
                               double distanceKm) {
            this.name        = name;
            this.category    = category;
            this.latitude    = latitude;
            this.longitude   = longitude;
            this.distanceKm  = distanceKm;
        }

        @Override
        public String toString() {
            return String.format("POI{name='%s', category='%s', "
                    + "lat=%.5f, lon=%.5f, dist=%.2f km}",
                    name, category, latitude, longitude, distanceKm);
        }
    }
}