/**
 * Represents raw biometric data input
 */
public class BiometricData {
    private byte[] rawData;
    private String dataType;
    private long timestamp;
    
    public BiometricData(byte[] rawData, String dataType) {
        this.rawData = rawData;
        this.dataType = dataType;
        this.timestamp = System.currentTimeMillis();
    }
    
    public byte[] getRawData() {
        return rawData;
    }
    
    public String getDataType() {
        return dataType;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
}