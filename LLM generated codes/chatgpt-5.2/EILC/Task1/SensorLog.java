public final class SensorLog {

    public long timestampMillis;
    public int sensorId;
    public double value;
    public int statusCode;

    public SensorLog(long timestampMillis, int sensorId, double value, int statusCode) {
        this.timestampMillis = timestampMillis;
        this.sensorId = sensorId;
        this.value = value;
        this.statusCode = statusCode;
    }
}
