public final class NativeImageValidator {

    static {
        System.loadLibrary("image_validator"); // libimage_validator.so / .dll
    }

    /**
     * Validates image format (e.g., PNG, JPEG).
     */
    static native boolean isFormatSupported(byte[] imageData);

    /**
     * Validates image header integrity.
     */
    static native boolean isHeaderValid(byte[] imageData);
}
