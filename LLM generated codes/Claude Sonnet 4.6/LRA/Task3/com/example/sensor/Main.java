package com.example.sensor;

import java.time.Instant;

/**
 * End-to-end demonstration of sensor threshold inspection via JNI.
 */
public class Main {

    public static void main(String[] args) {
        long now = Instant.now().toEpochMilli();

        /* ── Sensor readings batch ────────────────────────────────────────── */
        SensorReading[] readings = {
            new SensorReading("TEMP_01",     72.3,   "Celsius",  now - 5000),  // OK
            new SensorReading("TEMP_01",     102.7,  "Celsius",  now - 4000),  // ABOVE_MAX (>100)
            new SensorReading("TEMP_01",     -5.0,   "Celsius",  now - 3000),  // BELOW_MIN (<0)
            new SensorReading("PRESSURE_03", 1015.0, "hPa",      now - 2000),  // OK
            new SensorReading("PRESSURE_03", 850.0,  "hPa",      now - 1000),  // BELOW_MIN (<950)
            new SensorReading("PRESSURE_03", 1060.0, "hPa",      now),         // ABOVE_MAX (>1050)
            new SensorReading("RPM_07",      3500.0, "RPM",      now - 500),   // OK
            new SensorReading("RPM_07",      7200.0, "RPM",      now - 200),   // ABOVE_MAX (>6000)
            new SensorReading("HUMIDITY_02", 55.0,   "Percent",  now - 100),   // no threshold — skipped
        };

        /* ── Threshold configurations ─────────────────────────────────────── */
        ThresholdConfig[] thresholds = {
            new ThresholdConfig("TEMP_01",     0.0,   100.0),
            new ThresholdConfig("PRESSURE_03", 950.0, 1050.0),
            new ThresholdConfig("RPM_07",      500.0, 6000.0),
        };

        /* ── Invoke native inspection ─────────────────────────────────────── */
        SensorInspector inspector = new SensorInspector();
        SensorAlert[]   alerts    = inspector.inspectReadings(readings, thresholds);

        /* ── Print results ────────────────────────────────────────────────── */
        System.out.printf("Inspection complete — %d violation(s) detected:%n%n",
                          alerts.length);

        for (SensorAlert alert : alerts) {
            System.out.println("  " + alert);
        }

        /*
         * Expected output (timestamps vary):
         *
         * Inspection complete — 5 violation(s) detected:
         *
         *   SensorAlert{id='TEMP_01',     value=102.7000 Celsius, threshold=100.0000, violation=ABOVE_MAX, time=...}
         *   SensorAlert{id='TEMP_01',     value=-5.0000  Celsius, threshold=0.0000,   violation=BELOW_MIN, time=...}
         *   SensorAlert{id='PRESSURE_03', value=850.0000 hPa,     threshold=950.0000, violation=BELOW_MIN, time=...}
         *   SensorAlert{id='PRESSURE_03', value=1060.000 hPa,     threshold=1050.000, violation=ABOVE_MAX, time=...}
         *   SensorAlert{id='RPM_07',      value=7200.000 RPM,     threshold=6000.000, violation=ABOVE_MAX, time=...}
         */
    }
}