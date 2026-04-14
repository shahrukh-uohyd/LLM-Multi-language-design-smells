import java.util.List;

/**
 * Represents the structured elements after parsing
 */
public class ParsedElements {
    private List<String> elements;
    private int elementCount;
    private String delimiterUsed;
    
    public ParsedElements(List<String> elements, String delimiterUsed) {
        this.elements = elements;
        this.elementCount = elements != null ? elements.size() : 0;
        this.delimiterUsed = delimiterUsed;
    }
    
    public List<String> getElements() {
        return elements;
    }
    
    public int getElementCount() {
        return elementCount;
    }
    
    public String getDelimiterUsed() {
        return delimiterUsed;
    }
}