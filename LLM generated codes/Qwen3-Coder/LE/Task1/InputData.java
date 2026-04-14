// InputData.java
public class InputData {
    private String rawData;
    private int size;
    
    public InputData(String rawData) {
        this.rawData = rawData;
        this.size = rawData != null ? rawData.length() : 0;
    }
    
    public String getRawData() {
        return rawData;
    }
    
    public int getSize() {
        return size;
    }
}