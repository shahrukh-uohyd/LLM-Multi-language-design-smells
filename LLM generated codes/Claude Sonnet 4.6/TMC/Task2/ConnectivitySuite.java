import java.util.Arrays;

/**
 * ConnectivitySuite
 *
 * Manages Bluetooth device pairing and Points-of-Interest search
 * through the InfotainmentBridge native layer.
 */
public class ConnectivitySuite {

    // MAC address pattern: "XX:XX:XX:XX:XX:XX" (case-insensitive hex)
    private static final String MAC_ADDRESS_REGEX =
            "^([0-9A-Fa-f]{2}:){5}[0-9A-Fa-f]{2}$";

    private final InfotainmentBridge bridge;

    /**
     * @param bridge  Shared InfotainmentBridge instance.
     */
    public ConnectivitySuite(InfotainmentBridge bridge) {
        if (bridge == null) {
            throw new IllegalArgumentException("InfotainmentBridge must not be null.");
        }
        this.bridge = bridge;
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Initiates Bluetooth pairing with the specified device.
     *
     * @param macAddress  Target device MAC address ("AA:BB:CC:DD:EE:FF").
     * @param pin         Pairing PIN string, or "" for Secure Simple Pairing.
     * @return            Pairing result, or a failed result on validation error.
     */
    public InfotainmentBridge.BluetoothPairingResult pairDevice(String macAddress,
                                                                String pin) {
        if (macAddress == null || !macAddress.matches(MAC_ADDRESS_REGEX)) {
            String msg = "Invalid MAC address: '" + macAddress + "'.";
            System.err.println("[Connectivity] " + msg);
            return new InfotainmentBridge.BluetoothPairingResult(false, msg, 0);
        }

        String pairingMode = (pin == null || pin.isEmpty())
                ? "Secure Simple Pairing (SSP)"
                : "Legacy PIN pairing";

        System.out.printf("[Connectivity] Pairing with %s via %s...%n",
                macAddress, pairingMode);

        // ── Native call ──────────────────────────────────────────────
        InfotainmentBridge.BluetoothPairingResult result =
                bridge.pairBluetoothDevice(macAddress, pin != null ? pin : "");
        // ────────────────────────────────────────────────────────────

        if (result.success) {
            System.out.printf("[Connectivity] ✓ Paired with %s. %s%n",
                    macAddress, result);
        } else {
            System.err.printf("[Connectivity] ✗ Pairing FAILED for %s. %s%n",
                    macAddress, result);
        }
        return result;
    }

    /**
     * Searches for Points of Interest near a given location.
     *
     * @param latitude   Centre latitude.
     * @param longitude  Centre longitude.
     * @param radiusKm   Search radius in kilometres (must be > 0).
     * @param category   POI category filter, or null for all categories.
     * @return           Array of {@link InfotainmentBridge.PointOfInterest} records.
     */
    public InfotainmentBridge.PointOfInterest[] findNearbyPOIs(double latitude,
                                                               double longitude,
                                                               double radiusKm,
                                                               String category) {
        if (radiusKm <= 0) {
            throw new IllegalArgumentException(
                    "Search radius must be positive, got: " + radiusKm);
        }

        System.out.printf("[Connectivity] Searching POIs near (%.5f, %.5f) "
                + "within %.1f km, category='%s'...%n",
                latitude, longitude, radiusKm,
                category != null ? category : "ALL");

        // ── Native call ──────────────────────────────────────────────
        InfotainmentBridge.PointOfInterest[] results =
                bridge.searchPointsOfInterest(latitude, longitude,
                                              radiusKm, category);
        // ────────────────────────────────────────────────────────────

        if (results == null || results.length == 0) {
            System.out.println("[Connectivity] No POIs found.");
            return new InfotainmentBridge.PointOfInterest[0];
        }

        System.out.println("[Connectivity] Found " + results.length + " POI(s):");
        for (int i = 0; i < results.length; i++) {
            System.out.printf("[Connectivity]   [%d] %s%n", i + 1, results[i]);
        }
        return results;
    }

    /**
     * Convenience: pair a device, then immediately search for nearby fuel
     * stations (a common post-pairing use case for a phone handoff).
     *
     * @param macAddress  Bluetooth device MAC address.
     * @param latitude    Current latitude for POI search.
     * @param longitude   Current longitude for POI search.
     */
    public void pairAndSearchFuelStations(String macAddress,
                                          double latitude,
                                          double longitude) {
        System.out.println("[Connectivity] === Pair & Fuel Station Search ===");
        InfotainmentBridge.BluetoothPairingResult pairingResult =
                pairDevice(macAddress, "");

        if (pairingResult.success) {
            findNearbyPOIs(latitude, longitude, 5.0, "FUEL_STATION");
        } else {
            System.err.println("[Connectivity] Skipping POI search — device not paired.");
        }
        System.out.println("[Connectivity] ===================================");
    }
}