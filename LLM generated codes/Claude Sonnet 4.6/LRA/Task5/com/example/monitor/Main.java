package com.example.monitor;

import java.time.Instant;

/**
 * End-to-end demonstration of device failure monitoring via JNI.
 */
public class Main {

    public static void main(String[] args) {
        long now = Instant.now().toEpochMilli();

        StatusUpdate[] updates = {

            // DEV_SENSOR_01 — 4 failures (2 consecutive run, then 2 more)
            new StatusUpdate("DEV_SENSOR_01", StatusUpdate.STATUS_OK,       "Normal operation",           now - 10_000),
            new StatusUpdate("DEV_SENSOR_01", StatusUpdate.STATUS_FAILURE,  "Voltage spike detected",     now -  9_000),
            new StatusUpdate("DEV_SENSOR_01", StatusUpdate.STATUS_FAILURE,  "Voltage still elevated",     now -  8_000),
            new StatusUpdate("DEV_SENSOR_01", StatusUpdate.STATUS_WARNING,  "Voltage stabilising",        now -  7_000),
            new StatusUpdate("DEV_SENSOR_01", StatusUpdate.STATUS_CRITICAL, "Hardware fault",             now -  6_000),
            new StatusUpdate("DEV_SENSOR_01", StatusUpdate.STATUS_FAILURE,  "Repeated fault condition",   now -  5_000),

            // GATEWAY_B3 — 3 consecutive critical failures
            new StatusUpdate("GATEWAY_B3",    StatusUpdate.STATUS_CRITICAL, "Link lost",                  now -  4_500),
            new StatusUpdate("GATEWAY_B3",    StatusUpdate.STATUS_CRITICAL, "Reconnect failed",           now -  3_500),
            new StatusUpdate("GATEWAY_B3",    StatusUpdate.STATUS_CRITICAL, "Still unreachable",          now -  2_500),

            // TEMP_PROBE_07 — only 1 failure (below threshold of 2)
            new StatusUpdate("TEMP_PROBE_07", StatusUpdate.STATUS_OK,       "Reading nominal",            now -  2_000),
            new StatusUpdate("TEMP_PROBE_07", StatusUpdate.STATUS_FAILURE,  "Sensor out of range",        now -  1_000),

            // PUMP_CTRL_02 — 2 failures (meets threshold of 2)
            new StatusUpdate("PUMP_CTRL_02",  StatusUpdate.STATUS_FAILURE,  "Flow rate too low",          now -  9_500),
            new StatusUpdate("PUMP_CTRL_02",  StatusUpdate.STATUS_OK,       "Flow restored",              now -  8_500),
            new StatusUpdate("PUMP_CTRL_02",  StatusUpdate.STATUS_FAILURE,  "Flow rate dropped again",    now -  1_500),

            // null guard — should be skipped without crashing
            null,
        };

        DeviceMonitor  monitor = new DeviceMonitor();
        FailureReport[] reports = monitor.analyzeUpdates(updates, /* threshold */ 2);

        System.out.printf("=== Failure Analysis (threshold=2) — %d device(s) flagged ===%n%n",
                          reports.length);

        for (FailureReport r : reports) {
            System.out.println(r);
        }

        /*
         * Expected output order (sorted by totalFailureCount desc):
         *
         *  DEV_SENSOR_01  → totalFailures=4, maxConsecutive=2
         *  GATEWAY_B3     → totalFailures=3, maxConsecutive=3
         *  PUMP_CTRL_02   → totalFailures=2, maxConsecutive=1
         *  TEMP_PROBE_07  → excluded (only 1 failure, below threshold=2)
         */
    }
}