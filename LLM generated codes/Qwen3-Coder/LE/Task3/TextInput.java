/**
 * Represents the input text for processing
 */
public class TextInput {
    private String text;
    private int length;
    
    public TextInput(String text) {
        this.text = text;
        this.length = text != null ? text.length() : 0;
    }
    
    public String getText() {
        return text;
    }
    
    public int getLength() {
        return length;
    }
}