package com.example.monitoring;

public class DeviceStatusUpdate {
    public final int deviceId;
    public final int statusCode;
    public final String description;
    public final long timestamp;

    public DeviceStatusUpdate(int deviceId,
                              int statusCode,
                              String description,
                              long timestamp) {
        this.deviceId = deviceId;
        this.statusCode = statusCode;
        this.description = description;
        this.timestamp = timestamp;
    }
}
