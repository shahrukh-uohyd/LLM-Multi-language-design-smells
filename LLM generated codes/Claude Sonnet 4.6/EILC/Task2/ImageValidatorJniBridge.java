package com.example.imageservice.jni;

import com.example.imageservice.model.UploadedImage;
import com.example.imageservice.model.ValidationResult;

import java.util.List;

/**
 * JNI bridge to the native C++ image validation library.
 *
 * <h3>Batch strategy</h3>
 * All image bytes are marshalled into a flat {@code byte[][]} array and
 * passed as a single JNI call.  The native side iterates the array,
 * validates each image, and constructs one {@link ValidationResult} object
 * per image via the cached constructor mid-id — avoiding repeated
 * Java-side reflection lookups on the hot path.
 *
 * <h3>Thread safety</h3>
 * The native library is stateless; concurrent calls from separate threads
 * are safe.
 */
public final class ImageValidatorJniBridge {

    static {
        System.loadLibrary("image_validator_native"); // libimage_validator_native.so
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Validates a batch of uploaded images using the native C++ library.
     *
     * @param images non-null, non-empty list of images to validate
     * @return one {@link ValidationResult} per input image, in the same order
     * @throws IllegalArgumentException if {@code images} is null or empty
     */
    public ValidationResult[] validateBatch(List<UploadedImage> images) {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("Image batch must not be null or empty");
        }

        final int n = images.size();

        // Flatten into parallel arrays for efficient JNI transfer.
        byte[][]  imageDataArray = new byte[n][];
        String[]  filenameArray  = new String[n];

        for (int i = 0; i < n; i++) {
            UploadedImage img = images.get(i);
            imageDataArray[i] = img.getData();
            filenameArray[i]  = img.getFilename();
        }

        // Delegate to native — returns one ValidationResult per image.
        return nativeValidateBatch(imageDataArray, filenameArray, n);
    }

    // ── Native declarations ───────────────────────────────────────────────────

    /**
     * Performs format and header validation on each image in the batch.
     *
     * @param imageData  2-D array of raw image bytes, one row per image
     * @param filenames  corresponding filenames (same index mapping)
     * @param count      number of images in the batch
     * @return array of {@link ValidationResult} objects, one per image
     */
    private native ValidationResult[] nativeValidateBatch(
            byte[][]  imageData,
            String[]  filenames,
            int       count);
}