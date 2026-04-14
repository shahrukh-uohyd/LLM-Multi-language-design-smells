// ValidationResult.java
import java.util.List;
import java.util.ArrayList;

public class ValidationResult {
    private boolean isValid;
    private String errorMessage;
    private String imageFile;
    
    public ValidationResult(boolean isValid, String errorMessage, String imageFile) {
        this.isValid = isValid;
        this.errorMessage = errorMessage;
        this.imageFile = imageFile;
    }
    
    public boolean isValid() { return isValid; }
    public String getErrorMessage() { return errorMessage; }
    public String getImageFile() { return imageFile; }
    
    @Override
    public String toString() {
        return String.format("Image: %s, Valid: %s, Error: %s", 
                           imageFile, isValid, errorMessage);
    }
}