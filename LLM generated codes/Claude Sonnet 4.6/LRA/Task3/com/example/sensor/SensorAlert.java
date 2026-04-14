package com.example.sensor;

/**
 * Describes a single threshold violation detected by the native inspection engine.
 * The C++ code constructs and populates instances of this class via JNI.
 */
public class SensorAlert {

    /** Violation type constants — must stay in sync with C++ ViolationType enum */
    public static final int VIOLATION_BELOW_MIN = 0;
    public static final int VIOLATION_ABOVE_MAX = 1;

    /** ID of the offending sensor */
    public String sensorId;

    /** The actual measurement that breached the threshold */
    public double measurementValue;

    /** Physical unit of the measurement */
    public String unit;

    /** Timestamp of the offending reading (epoch millis) */
    public long collectionTime;

    /** Threshold that was violated (lower or upper bound) */
    public double thresholdViolated;

    /** Direction of the violation: VIOLATION_BELOW_MIN or VIOLATION_ABOVE_MAX */
    public int violationType;

    /** Zero-arg constructor required by JNI (NewObject) */
    public SensorAlert() {}

    @Override
    public String toString() {
        String direction = (violationType == VIOLATION_BELOW_MIN) ? "BELOW_MIN" : "ABOVE_MAX";
        return String.format(
            "SensorAlert{id='%s', value=%.4f %s, threshold=%.4f, violation=%s, time=%d}",
            sensorId, measurementValue, unit, thresholdViolated, direction, collectionTime);
    }
}