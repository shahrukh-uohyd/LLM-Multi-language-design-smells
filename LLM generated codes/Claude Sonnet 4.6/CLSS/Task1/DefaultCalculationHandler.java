package com.example.jni;

/**
 * A default implementation of CalculationStatusHandler that logs status updates.
 */
public class DefaultCalculationHandler implements CalculationStatusHandler {

    @Override
    public void onStatusUpdate(int statusCode, String description) {
        System.out.printf("[Native Callback] Status Code: %d | Message: %s%n",
                statusCode, description);
    }
}