// NormalizedData.java
public class NormalizedData {
    private Object[] normalizedElements;
    private String normalizationRules;
    
    public NormalizedData(Object[] normalizedElements, String normalizationRules) {
        this.normalizedElements = normalizedElements;
        this.normalizationRules = normalizationRules;
    }
    
    public Object[] getNormalizedElements() {
        return normalizedElements;
    }
    
    public String getNormalizationRules() {
        return normalizationRules;
    }
}