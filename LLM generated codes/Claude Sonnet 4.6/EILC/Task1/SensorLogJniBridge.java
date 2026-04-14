package com.example.datalogger.jni;

import com.example.datalogger.model.SensorLog;
import java.util.List;

/**
 * JNI bridge that delegates sensor log persistence to the native C++ database.
 *
 * <p>Usage:
 * <pre>
 *   SensorLogJniBridge bridge = new SensorLogJniBridge(dbPath);
 *   bridge.syncLogs(capturedLogs);   // synchronize 30 logs
 *   bridge.close();
 * </pre>
 */
public class SensorLogJniBridge implements AutoCloseable {

    static {
        System.loadLibrary("sensorlog_native"); // loads libsensorlog_native.so
    }

    // Opaque pointer to the native database handle (stored as a long)
    private long nativeDbHandle = 0;

    // ── Lifecycle ───────────────────────────────────���────────────────────────

    /**
     * Opens (or creates) the native database at the given file path.
     *
     * @param dbPath absolute path to the database file on disk
     * @throws RuntimeException if the native database cannot be opened
     */
    public SensorLogJniBridge(String dbPath) {
        nativeDbHandle = nativeOpenDatabase(dbPath);
        if (nativeDbHandle == 0) {
            throw new RuntimeException("Failed to open native database at: " + dbPath);
        }
    }

    /**
     * Synchronizes a batch of {@link SensorLog} objects with the native database.
     *
     * <p>All 30 logs are written inside a single native transaction for atomicity
     * and maximum throughput.
     *
     * @param logs the collection of sensor logs to persist (expected size: 30)
     * @return number of logs successfully written
     * @throws IllegalArgumentException if {@code logs} is null or empty
     * @throws IllegalStateException    if the bridge has already been closed
     */
    public int syncLogs(List<SensorLog> logs) {
        if (logs == null || logs.isEmpty()) {
            throw new IllegalArgumentException("logs must not be null or empty");
        }
        if (nativeDbHandle == 0) {
            throw new IllegalStateException("Database handle is closed");
        }

        // Flatten the list into parallel primitive arrays for efficient JNI transfer.
        // Crossing the JNI boundary with arrays is significantly faster than
        // passing individual Java objects in a loop.
        final int size = logs.size();

        long[]   timestamps = new long[size];
        String[] sensorIds  = new String[size];
        float[]  values     = new float[size];
        String[] units      = new String[size];
        int[]    statuses   = new int[size];

        for (int i = 0; i < size; i++) {
            SensorLog log = logs.get(i);
            timestamps[i] = log.getTimestampMs();
            sensorIds[i]  = log.getSensorId();
            values[i]     = log.getValue();
            units[i]      = log.getUnit();
            statuses[i]   = log.getStatus();
        }

        return nativeSyncLogs(
            nativeDbHandle,
            timestamps,
            sensorIds,
            values,
            units,
            statuses,
            size
        );
    }

    @Override
    public void close() {
        if (nativeDbHandle != 0) {
            nativeCloseDatabase(nativeDbHandle);
            nativeDbHandle = 0;
        }
    }

    // ── Native declarations ───────────────────────────────────────────────────

    /**
     * Opens the native database and returns an opaque handle.
     * Returns 0 on failure.
     */
    private native long nativeOpenDatabase(String dbPath);

    /**
     * Writes a batch of sensor logs atomically to the native database.
     *
     * @return number of rows actually inserted/updated
     */
    private native int nativeSyncLogs(
        long     dbHandle,
        long[]   timestamps,
        String[] sensorIds,
        float[]  values,
        String[] units,
        int[]    statuses,
        int      count
    );

    /**
     * Releases all native resources held by the database handle.
     */
    private native void nativeCloseDatabase(long dbHandle);
}