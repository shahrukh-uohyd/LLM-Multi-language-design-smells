/**
 * DashboardDisplay
 *
 * Reads live vehicle telemetry (fuel level and engine temperature)
 * from the CAN bus via InfotainmentBridge and presents them to the UI.
 *
 * Sensor index convention used by this vehicle platform:
 *   Sensor 0 → primary fuel tank
 *   Sensor 1 → engine coolant / temperature sensor
 */
public class DashboardDisplay {

    // Warning thresholds
    private static final float FUEL_LOW_THRESHOLD_PERCENT  = 15.0f;
    private static final float ENGINE_TEMP_HIGH_THRESHOLD_C = 105.0f;

    private final InfotainmentBridge bridge;

    /**
     * @param bridge  Shared InfotainmentBridge instance.
     */
    public DashboardDisplay(InfotainmentBridge bridge) {
        if (bridge == null) {
            throw new IllegalArgumentException("InfotainmentBridge must not be null.");
        }
        this.bridge = bridge;
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Refreshes and displays the current fuel level.
     *
     * @param sensorId  Fuel sensor index.
     * @return          Fuel percentage [0.0 – 100.0], or -1.0 on error.
     */
    public float displayFuelLevel(int sensorId) {
        System.out.println("[Dashboard] Querying fuel sensor " + sensorId + "...");

        // ── Native call ──────────────────────────────────────────────
        float fuelPercent = bridge.readFuelLevel(sensorId);
        // ──────────────���─────────────────────────────────────────────

        if (fuelPercent < 0) {
            System.err.println("[Dashboard] ERROR: Fuel sensor " + sensorId
                    + " is unavailable.");
            return fuelPercent;
        }

        System.out.printf("[Dashboard] Fuel Level: %.1f%%%n", fuelPercent);

        if (fuelPercent <= FUEL_LOW_THRESHOLD_PERCENT) {
            System.out.printf("[Dashboard] ⚠  WARNING: Low fuel — %.1f%% remaining.%n",
                    fuelPercent);
        }
        return fuelPercent;
    }

    /**
     * Refreshes and displays the current engine temperature.
     *
     * @param sensorId  Temperature sensor index.
     * @return          Temperature in °C, or Float.NaN on error.
     */
    public float displayEngineTemperature(int sensorId) {
        System.out.println("[Dashboard] Querying engine temperature sensor "
                + sensorId + "...");

        // ── Native call ──────────────────────────────────────────────
        float tempCelsius = bridge.readEngineTemperature(sensorId);
        // ────────────────────────────────────────────────────────────

        if (Float.isNaN(tempCelsius)) {
            System.err.println("[Dashboard] ERROR: Engine temperature sensor "
                    + sensorId + " is unavailable.");
            return tempCelsius;
        }

        System.out.printf("[Dashboard] Engine Temp: %.1f °C%n", tempCelsius);

        if (tempCelsius >= ENGINE_TEMP_HIGH_THRESHOLD_C) {
            System.out.printf("[Dashboard] 🔴 CRITICAL: Engine overheating — %.1f °C!%n",
                    tempCelsius);
        }
        return tempCelsius;
    }

    /**
     * Convenience method: refresh both readings in one call.
     */
    public void refreshAll() {
        System.out.println("[Dashboard] === Full Dashboard Refresh ===");
        displayFuelLevel(0);
        displayEngineTemperature(1);
        System.out.println("[Dashboard] === Refresh Complete ===");
    }
}