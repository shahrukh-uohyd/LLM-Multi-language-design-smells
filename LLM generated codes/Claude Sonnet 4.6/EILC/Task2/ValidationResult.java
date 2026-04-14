package com.example.imageservice.model;

/**
 * Describes the outcome of validating a single {@link UploadedImage}.
 *
 * <p>Produced by the native validator and handed back to Java through the
 * JNI bridge.
 */
public final class ValidationResult {

    // ── Error-code constants (must mirror native_validator.h) ────────────────
    public static final int ERR_NONE              = 0;
    public static final int ERR_UNSUPPORTED_FORMAT = 1;
    public static final int ERR_INVALID_HEADER     = 2;
    public static final int ERR_DATA_TOO_SHORT      = 3;
    public static final int ERR_CORRUPTED_DATA      = 4;

    private final String  filename;
    private final boolean valid;
    private final int     errorCode;
    private final String  errorMessage;
    private final int     detectedFormatOrdinal;   // maps to ImageFormat ordinal

    /**
     * Called reflectively by the native layer via JNI to populate each result.
     */
    public ValidationResult(String filename,
                             boolean valid,
                             int errorCode,
                             String errorMessage,
                             int detectedFormatOrdinal) {
        this.filename              = filename;
        this.valid                 = valid;
        this.errorCode             = errorCode;
        this.errorMessage          = errorMessage;
        this.detectedFormatOrdinal = detectedFormatOrdinal;
    }

    public String      getFilename()      { return filename; }
    public boolean     isValid()          { return valid; }
    public int         getErrorCode()     { return errorCode; }
    public String      getErrorMessage()  { return errorMessage; }
    public ImageFormat getDetectedFormat() {
        ImageFormat[] values = ImageFormat.values();
        if (detectedFormatOrdinal >= 0 && detectedFormatOrdinal < values.length) {
            return values[detectedFormatOrdinal];
        }
        return ImageFormat.UNKNOWN;
    }

    @Override
    public String toString() {
        return String.format("[%s] valid=%b format=%s errorCode=%d msg=\"%s\"",
            filename, valid, getDetectedFormat(), errorCode, errorMessage);
    }
}