package com.example.monitor;

/**
 * Represents a single device status update captured by the monitoring system.
 *
 * <p>Status codes follow a well-known convention:
 * <ul>
 *   <li>{@link #STATUS_OK}       — device is healthy</li>
 *   <li>{@link #STATUS_WARNING}  — elevated concern, not yet failing</li>
 *   <li>{@link #STATUS_FAILURE}  — device is in a failure state</li>
 *   <li>{@link #STATUS_CRITICAL} — device in critical / unrecoverable state</li>
 *   <li>{@link #STATUS_UNKNOWN}  — status cannot be determined</li>
 * </ul>
 * Status codes {@link #STATUS_FAILURE} and {@link #STATUS_CRITICAL} are treated
 * as failure states by the native inspection engine.
 * </p>
 */
public class StatusUpdate {

    // -------------------------------------------------------------------------
    // Status code constants — MUST stay in sync with C++ StatusCode enum
    // -------------------------------------------------------------------------
    public static final int STATUS_OK       = 0;
    public static final int STATUS_WARNING  = 1;
    public static final int STATUS_FAILURE  = 2;
    public static final int STATUS_CRITICAL = 3;
    public static final int STATUS_UNKNOWN  = 4;

    /** Unique device identifier (e.g. "DEV_SENSOR_01", "GATEWAY_B3") */
    public final String deviceId;

    /**
     * One of the STATUS_* constants above.
     * The native layer treats STATUS_FAILURE and STATUS_CRITICAL as failures.
     */
    public final int statusCode;

    /** Human-readable description of the current status event */
    public final String description;

    /** UTC timestamp of this update as Unix epoch milliseconds */
    public final long updateTime;

    public StatusUpdate(String deviceId,
                        int    statusCode,
                        String description,
                        long   updateTime) {

        if (deviceId == null || deviceId.isBlank())
            throw new IllegalArgumentException("deviceId must not be null or blank");
        if (description == null)
            throw new IllegalArgumentException("description must not be null");

        this.deviceId    = deviceId;
        this.statusCode  = statusCode;
        this.description = description;
        this.updateTime  = updateTime;
    }

    @Override
    public String toString() {
        return String.format("StatusUpdate{deviceId='%s', code=%d, time=%d, desc='%s'}",
                             deviceId, statusCode, updateTime, description);
    }
}