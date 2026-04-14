import java.util.Map;

/**
 * Represents data after interpretation from low-level format
 */
public class InterpretedData {
    private Map<String, Object> fields;
    private String dataType;
    private long timestamp;
    
    public InterpretedData(Map<String, Object> fields, String dataType) {
        this.fields = fields;
        this.dataType = dataType;
        this.timestamp = System.currentTimeMillis();
    }
    
    public Map<String, Object> getFields() {
        return fields;
    }
    
    public String getDataType() {
        return dataType;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public Object getField(String key) {
        return fields.get(key);
    }
}