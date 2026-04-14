public final class ImageValidationResult {

    private final String imageName;
    private final boolean valid;
    private final String errorMessage;

    public ImageValidationResult(String imageName, boolean valid, String errorMessage) {
        this.imageName = imageName;
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    public String getImageName() {
        return imageName;
    }

    public boolean isValid() {
        return valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
