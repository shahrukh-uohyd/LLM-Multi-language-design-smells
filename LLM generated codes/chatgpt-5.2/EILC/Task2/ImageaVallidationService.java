import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ImageValidationService {

    /**
     * Validates a batch of uploaded images.
     *
     * @param images map of image name → raw image bytes
     * @return list of validation results (one per image)
     */
    public List<ImageValidationResult> validateBatch(Map<String, byte[]> images) {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("Image batch must not be null or empty");
        }

        List<ImageValidationResult> results = new ArrayList<>(images.size());

        for (Map.Entry<String, byte[]> entry : images.entrySet()) {
            String imageName = entry.getKey();
            byte[] imageData = entry.getValue();

            if (imageData == null || imageData.length == 0) {
                results.add(new ImageValidationResult(
                        imageName,
                        false,
                        "Empty or missing image data"
                ));
                continue;
            }

            boolean formatOk;
            boolean headerOk;

            try {
                formatOk = NativeImageValidator.isFormatSupported(imageData);
                headerOk = NativeImageValidator.isHeaderValid(imageData);
            } catch (RuntimeException nativeFailure) {
                results.add(new ImageValidationResult(
                        imageName,
                        false,
                        "Native validation error: " + nativeFailure.getMessage()
                ));
                continue;
            }

            if (!formatOk) {
                results.add(new ImageValidationResult(
                        imageName,
                        false,
                        "Unsupported image format"
                ));
            } else if (!headerOk) {
                results.add(new ImageValidationResult(
                        imageName,
                        false,
                        "Invalid image header"
                ));
            } else {
                results.add(new ImageValidationResult(
                        imageName,
                        true,
                        null
                ));
            }
        }

        return results;
    }
}
