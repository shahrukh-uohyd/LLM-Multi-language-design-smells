public class ImageBatchValidator {
    static {
        System.loadLibrary("imagevalidator");
    }

    /**
     * Validates a batch of images natively.
     * @param images The array of images to validate.
     * @return An array containing ValidationResults ONLY for the invalid images. 
     *         Returns an empty array if all images are valid.
     */
    public native ValidationResult[] validateImageBatch(ImageRecord[] images);
}