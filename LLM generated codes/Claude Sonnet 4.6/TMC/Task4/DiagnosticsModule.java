/**
 * DiagnosticsModule
 *
 * Reads battery voltage and internal temperature telemetry from
 * the native sensor bus via {@link RobotBridge} and evaluates them
 * against configurable safe-operating thresholds.
 */
public class DiagnosticsModule {

    // ----------------------------------------------------------------
    // Safe-operating thresholds
    // ----------------------------------------------------------------
    /** Minimum acceptable battery voltage before a low-power warning. */
    public static final float BATTERY_VOLTAGE_LOW_V      = 21.0f;
    /** Critical battery voltage — robot must return to charging dock. */
    public static final float BATTERY_VOLTAGE_CRITICAL_V = 18.0f;
    /** Maximum safe internal temperature before throttling. */
    public static final float TEMP_HIGH_THRESHOLD_C      = 70.0f;
    /** Critical temperature — hard shutdown must be initiated. */
    public static final float TEMP_CRITICAL_C            = 85.0f;

    private final RobotBridge bridge;

    /**
     * @param bridge  Shared {@link RobotBridge} instance.
     */
    public DiagnosticsModule(RobotBridge bridge) {
        if (bridge == null) {
            throw new IllegalArgumentException("RobotBridge must not be null.");
        }
        this.bridge = bridge;
    }

    // ----------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------

    /**
     * Reads and evaluates the battery voltage for a given cell group.
     *
     * @param cellGroupId  Cell group index; 0 = aggregate pack voltage.
     * @return             Voltage in Volts, or {@code -1.0f} on sensor error.
     */
    public float checkBatteryVoltage(int cellGroupId) {
        if (cellGroupId < 0) {
            throw new IllegalArgumentException(
                "Cell group ID must be non-negative, got: " + cellGroupId);
        }

        System.out.printf("[Diagnostics] Reading battery voltage "
                + "(cell group %d)...%n", cellGroupId);

        // ── Native call ──────────────────────────────────────────────
        float voltage = bridge.checkBatteryVoltage(cellGroupId);
        // ────────────────────────────────────────────────────────────

        if (voltage < 0) {
            System.err.printf("[Diagnostics] ERROR: Battery sensor (group %d) "
                    + "unresponsive.%n", cellGroupId);
            return voltage;
        }

        System.out.printf("[Diagnostics] Battery voltage: %.2f V%n", voltage);

        if (voltage <= BATTERY_VOLTAGE_CRITICAL_V) {
            System.err.printf("[Diagnostics] 🔴 CRITICAL: Battery at %.2f V — "
                    + "return to dock immediately!%n", voltage);
        } else if (voltage <= BATTERY_VOLTAGE_LOW_V) {
            System.err.printf("[Diagnostics] ⚠  WARNING: Low battery — %.2f V.%n",
                    voltage);
        }
        return voltage;
    }

    /**
     * Reads and evaluates the internal temperature at a sensor node.
     *
     * @param sensorNodeId  I²C node index of the temperature sensor.
     * @return              Temperature in °C, or {@link Float#NaN} if offline.
     */
    public float getInternalTemperature(int sensorNodeId) {
        if (sensorNodeId < 0) {
            throw new IllegalArgumentException(
                "Sensor node ID must be non-negative, got: " + sensorNodeId);
        }

        System.out.printf("[Diagnostics] Reading temperature "
                + "(sensor node %d)...%n", sensorNodeId);

        // ── Native call ──────────────────────────────────────────────
        float tempC = bridge.getInternalTemperature(sensorNodeId);
        // ────────────────────────────────────────────────────────────

        if (Float.isNaN(tempC)) {
            System.err.printf("[Diagnostics] ERROR: Temperature sensor node %d "
                    + "is offline.%n", sensorNodeId);
            return tempC;
        }

        System.out.printf("[Diagnostics] Internal temperature: %.1f °C%n", tempC);

        if (tempC >= TEMP_CRITICAL_C) {
            System.err.printf("[Diagnostics] 🔴 CRITICAL: Temperature %.1f °C — "
                    + "initiating emergency shutdown!%n", tempC);
        } else if (tempC >= TEMP_HIGH_THRESHOLD_C) {
            System.err.printf("[Diagnostics] ⚠  WARNING: High temperature — "
                    + "%.1f °C. Throttling performance.%n", tempC);
        }
        return tempC;
    }

    /**
     * Runs a full health check across all monitored cell groups and
     * temperature sensor nodes and returns a consolidated report.
     *
     * @param cellGroupCount   Number of battery cell groups to poll.
     * @param tempSensorCount  Number of temperature sensor nodes to poll.
     * @return                 {@link HealthReport} summarising system status.
     */
    public HealthReport runFullHealthCheck(int cellGroupCount, int tempSensorCount) {
        System.out.println("[Diagnostics] === Full Health Check ===");

        float   minVoltage = Float.MAX_VALUE;
        boolean voltageOk  = true;
        for (int i = 0; i < cellGroupCount; i++) {
            float v = checkBatteryVoltage(i);
            if (v < 0) continue;                          // sensor offline
            if (v < minVoltage) minVoltage = v;
            if (v <= BATTERY_VOLTAGE_CRITICAL_V) voltageOk = false;
        }

        float   maxTemp  = -Float.MAX_VALUE;
        boolean thermalOk = true;
        for (int i = 0; i < tempSensorCount; i++) {
            float t = getInternalTemperature(i);
            if (Float.isNaN(t)) continue;                 // sensor offline
            if (t > maxTemp) maxTemp = t;
            if (t >= TEMP_CRITICAL_C) thermalOk = false;
        }

        HealthReport report = new HealthReport(
            minVoltage == Float.MAX_VALUE ? -1f : minVoltage,
            maxTemp    == -Float.MAX_VALUE ? Float.NaN : maxTemp,
            voltageOk,
            thermalOk);

        System.out.println("[Diagnostics] === Health Check Complete: "
                + report + " ===");
        return report;
    }

    // ----------------------------------------------------------------
    // Nested result type
    // ----------------------------------------------------------------

    /** Consolidated result from {@link #runFullHealthCheck}. */
    public static class HealthReport {
        /** Lowest voltage reading across all cell groups (V). */
        public final float   minBatteryVoltage;
        /** Highest temperature reading across all sensor nodes (°C). */
        public final float   maxTemperature;
        /** false if any cell group is at or below the critical voltage. */
        public final boolean batteryHealthy;
        /** false if any node has reached the critical temperature. */
        public final boolean thermalHealthy;

        public HealthReport(float minBatteryVoltage, float maxTemperature,
                            boolean batteryHealthy,  boolean thermalHealthy) {
            this.minBatteryVoltage = minBatteryVoltage;
            this.maxTemperature    = maxTemperature;
            this.batteryHealthy    = batteryHealthy;
            this.thermalHealthy    = thermalHealthy;
        }

        /** @return true only if both battery and thermal states are healthy. */
        public boolean isFullyHealthy() {
            return batteryHealthy && thermalHealthy;
        }

        @Override
        public String toString() {
            return String.format(
                "HealthReport{minV=%.2f V, maxT=%.1f °C, "
                + "battOK=%b, thermOK=%b}",
                minBatteryVoltage, maxTemperature,
                batteryHealthy, thermalHealthy);
        }
    }
}