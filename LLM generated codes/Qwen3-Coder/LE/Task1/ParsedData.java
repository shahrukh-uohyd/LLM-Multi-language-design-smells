// ParsedData.java
public class ParsedData {
    private Object[] elements;
    private int count;
    
    public ParsedData(Object[] elements) {
        this.elements = elements;
        this.count = elements != null ? elements.length : 0;
    }
    
    public Object[] getElements() {
        return elements;
    }
    
    public int getCount() {
        return count;
    }
}