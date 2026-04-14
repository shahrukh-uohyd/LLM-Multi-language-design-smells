public class ValidationResult {
    public String imageId;
    public String errorMessage;

    public ValidationResult(String imageId, String errorMessage) {
        this.imageId = imageId;
        this.errorMessage = errorMessage;
    }
}