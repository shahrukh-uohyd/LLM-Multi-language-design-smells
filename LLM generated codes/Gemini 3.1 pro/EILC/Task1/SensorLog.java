public class SensorLog {
    public long timestamp;
    public int sensorId;
    public float value;

    public SensorLog(long timestamp, int sensorId, float value) {
        this.timestamp = timestamp;
        this.sensorId = sensorId;
        this.value = value;
    }
}