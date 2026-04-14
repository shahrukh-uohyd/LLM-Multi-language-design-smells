package com.example.monitoring;

public class DeviceStatusUpdate {
    public static final int STATUS_OK = 0;
    public static final int STATUS_WARNING = 1;
    public static final int STATUS_FAILURE = 2;

    public String deviceId;
    public int statusCode;
    public String description;
    public long updateTime;

    public DeviceStatusUpdate(String deviceId, int statusCode, String description, long updateTime) {
        this.deviceId = deviceId;
        this.statusCode = statusCode;
        this.description = description;
        this.updateTime = updateTime;
    }
}