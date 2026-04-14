package com.example.datalogger.model;

/**
 * Represents a single captured sensor reading.
 */
public class SensorLog {
    private final long   timestampMs;   // epoch milliseconds
    private final String sensorId;      // unique sensor identifier
    private final float  value;         // measured value
    private final String unit;          // unit of measurement (e.g., "°C", "hPa")
    private final int    status;        // 0 = OK, non-zero = error code

    public SensorLog(long timestampMs, String sensorId,
                     float value, String unit, int status) {
        this.timestampMs = timestampMs;
        this.sensorId    = sensorId;
        this.value       = value;
        this.unit        = unit;
        this.status      = status;
    }

    public long   getTimestampMs() { return timestampMs; }
    public String getSensorId()    { return sensorId;    }
    public float  getValue()       { return value;       }
    public String getUnit()        { return unit;        }
    public int    getStatus()      { return status;      }
}