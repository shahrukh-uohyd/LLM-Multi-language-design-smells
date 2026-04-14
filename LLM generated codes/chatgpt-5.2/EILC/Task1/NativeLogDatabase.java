public final class NativeLogDatabase {

    static {
        // libsensor_db.so / sensor_db.dll
        System.loadLibrary("sensor_db");
    }

    /**
     * Native method that persists a batch of logs.
     *
     * Expected layout (arrays must all be length 30):
     *  - timestamps[i]
     *  - sensorIds[i]
     *  - values[i]
     *  - statusCodes[i]
     */
    private static native void persistLogsNative(
            long[] timestamps,
            int[] sensorIds,
            double[] values,
            int[] statusCodes
    );

    /**
     * Synchronizes exactly 30 SensorLog objects with the native database.
     */
    public static void syncLogs(SensorLog[] logs) {
        if (logs == null || logs.length != 30) {
            throw new IllegalArgumentException("Exactly 30 SensorLog entries are required");
        }

        // Prepare primitive arrays for JNI
        long[] timestamps = new long[30];
        int[] sensorIds   = new int[30];
        double[] values   = new double[30];
        int[] statusCodes = new int[30];

        for (int i = 0; i < 30; i++) {
            SensorLog log = logs[i];
            if (log == null) {
                throw new IllegalArgumentException("SensorLog at index " + i + " is null");
            }

            timestamps[i] = log.timestampMillis;
            sensorIds[i]  = log.sensorId;
            values[i]     = log.value;
            statusCodes[i]= log.statusCode;
        }

        // Single JNI call — atomic from Java's perspective
        persistLogsNative(timestamps, sensorIds, values, statusCodes);
    }
}
