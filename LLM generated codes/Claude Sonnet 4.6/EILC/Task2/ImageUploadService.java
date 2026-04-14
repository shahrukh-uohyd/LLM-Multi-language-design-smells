package com.example.imageservice;

import com.example.imageservice.jni.ImageValidatorJniBridge;
import com.example.imageservice.model.UploadedImage;
import com.example.imageservice.model.ValidationResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Service that validates a batch of uploaded images before further processing.
 *
 * <h3>Flow</h3>
 * <pre>
 *   receive upload batch
 *        │
 *        ▼
 *   validateBatch()  ──► JNI ──► C++ native validator
 *        │
 *        ├── valid images   ──► processValidImages()
 *        └── invalid images ──► reportInvalidImages()
 * </pre>
 */
public class ImageUploadService {

    private final ImageValidatorJniBridge validator = new ImageValidatorJniBridge();

    /**
     * Entry point: validates the batch and routes each image to the
     * appropriate downstream handler.
     *
     * @param batch list of images received from the upload endpoint
     */
    public void handleUploadBatch(List<UploadedImage> batch) {
        if (batch == null || batch.isEmpty()) {
            System.out.println("[ImageUploadService] Received an empty batch — nothing to do.");
            return;
        }

        System.out.printf("[ImageUploadService] Validating batch of %d image(s)...%n", batch.size());

        // ── 1. Run native validation ─────────────────────────────────────────
        ValidationResult[] results = validator.validateBatch(batch);

        // ── 2. Partition results ─────────────────────────────────────────────
        List<UploadedImage>    validImages   = new ArrayList<>();
        List<ValidationResult> invalidImages = new ArrayList<>();

        for (int i = 0; i < results.length; i++) {
            ValidationResult result = results[i];
            if (result.isValid()) {
                validImages.add(batch.get(i));
            } else {
                invalidImages.add(result);
            }
        }

        // ── 3. Report invalid images ─────────────────────────────────────────
        if (!invalidImages.isEmpty()) {
            reportInvalidImages(invalidImages);
        }

        // ── 4. Forward valid images for further processing ───────────────────
        if (!validImages.isEmpty()) {
            processValidImages(validImages);
        }

        System.out.printf("[ImageUploadService] Done: %d valid, %d invalid.%n",
                validImages.size(), invalidImages.size());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void reportInvalidImages(List<ValidationResult> invalids) {
        System.err.printf("[ImageUploadService] %d image(s) failed validation:%n", invalids.size());
        for (ValidationResult r : invalids) {
            System.err.printf("  ✗ %s%n", r);
        }
    }

    private void processValidImages(List<UploadedImage> valids) {
        // Downstream pipeline: storage, thumbnail generation, ML inference, etc.
        System.out.printf("[ImageUploadService] Processing %d valid image(s)...%n", valids.size());
        for (UploadedImage img : valids) {
            System.out.printf("  ✓ %s (%d bytes)%n", img.getFilename(), img.getSize());
        }
    }
}