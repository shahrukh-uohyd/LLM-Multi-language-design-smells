// ImageValidationService.java
import java.util.List;
import java.util.ArrayList;

public class ImageValidationService {
    static {
        System.loadLibrary("image_validator"); // Load native library
    }
    
    // Native method declarations - Individual validation approach
    public native boolean validateImageHeader(String imagePath);
    public native boolean isSupportedFormat(String imagePath);
    public native String getValidationError(String imagePath);
    
    // Batch validation method - validates all images in one call
    public native ValidationResult[] validateImageBatch(String[] imagePaths);
    
    /**
     * Validate a single image individually
     */
    public ValidationResult validateSingleImage(ImageInfo image) {
        boolean hasValidHeader = validateImageHeader(image.getImagePath());
        boolean isSupported = isSupportedFormat(image.getImagePath());
        
        if (!hasValidHeader) {
            String error = getValidationError(image.getImagePath());
            return new ValidationResult(false, "Invalid image header: " + error, image.getFileName());
        }
        
        if (!isSupported) {
            return new ValidationResult(false, "Unsupported image format", image.getFileName());
        }
        
        return new ValidationResult(true, null, image.getFileName());
    }
    
    /**
     * Validate a batch of images using individual validation
     */
    public List<ValidationResult> validateBatchIndividually(List<ImageInfo> images) {
        List<ValidationResult> results = new ArrayList<>();
        
        for (ImageInfo image : images) {
            ValidationResult result = validateSingleImage(image);
            results.add(result);
        }
        
        return results;
    }
    
    /**
     * Validate a batch of images using native batch validation (more efficient)
     */
    public List<ValidationResult> validateBatchEfficiently(List<ImageInfo> images) {
        if (images.isEmpty()) {
            return new ArrayList<>();
        }
        
        String[] imagePaths = images.stream()
                                   .map(ImageInfo::getImagePath)
                                   .toArray(String[]::new);
        
        ValidationResult[] nativeResults = validateImageBatch(imagePaths);
        List<ValidationResult> results = new ArrayList<>();
        
        // Map results back to original image names
        for (int i = 0; i < nativeResults.length && i < images.size(); i++) {
            String fileName = images.get(i).getFileName();
            // Create new result with original filename
            results.add(new ValidationResult(
                nativeResults[i].isValid(),
                nativeResults[i].getErrorMessage(),
                fileName
            ));
        }
        
        return results;
    }
    
    /**
     * Get only invalid images from batch validation
     */
    public List<ValidationResult> getInvalidImages(List<ImageInfo> images) {
        List<ValidationResult> allResults = validateBatchEfficiently(images);
        List<ValidationResult> invalidResults = new ArrayList<>();
        
        for (ValidationResult result : allResults) {
            if (!result.isValid()) {
                invalidResults.add(result);
            }
        }
        
        return invalidResults;
    }
}