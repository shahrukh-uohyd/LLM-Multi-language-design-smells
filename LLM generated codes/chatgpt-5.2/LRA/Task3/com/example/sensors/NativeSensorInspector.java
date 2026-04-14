package com.example.sensors;

public class NativeSensorInspector {

    static {
        System.loadLibrary("sensorinspect");
    }

    /**
     * @param readings array of sensor readings
     * @param thresholds threshold values indexed by sensorId
     * @return boolean[] where true means reading exceeds threshold
     */
    public static native boolean[] inspect(
            SensorReading[] readings,
            double[] thresholds);
}
