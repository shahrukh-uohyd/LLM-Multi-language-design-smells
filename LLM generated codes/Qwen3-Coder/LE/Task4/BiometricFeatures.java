import java.util.List;

/**
 * Represents extracted biometric features/minutiae
 */
public class BiometricFeatures {
    private List<String> features;
    private String extractionMethod;
    private int qualityScore;
    
    public BiometricFeatures(List<String> features, String extractionMethod, int qualityScore) {
        this.features = features;
        this.extractionMethod = extractionMethod;
        this.qualityScore = qualityScore;
    }
    
    public List<String> getFeatures() {
        return features;
    }
    
    public String getExtractionMethod() {
        return extractionMethod;
    }
    
    public int getQualityScore() {
        return qualityScore;
    }
}