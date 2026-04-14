package com.example.jni;

/**
 * Callback interface for receiving status updates from the native calculation layer.
 */
public interface CalculationStatusHandler {
    /**
     * Called periodically by the native layer during a long-running calculation.
     *
     * @param statusCode  An integer code representing the current state/progress.
     * @param description A human-readable description of the current status.
     */
    void onStatusUpdate(int statusCode, String description);
}