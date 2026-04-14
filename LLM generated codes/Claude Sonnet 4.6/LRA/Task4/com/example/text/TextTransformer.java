package com.example.text;

/**
 * JNI bridge for native batch text transformation.
 *
 * <p>The native C++ library performs the uppercase conversion,
 * allowing integration with platform-native or ICU-based Unicode
 * processing pipelines.</p>
 */
public class TextTransformer {

    static {
        // Loads libtext_transformer.so (Linux/macOS) or text_transformer.dll (Windows)
        System.loadLibrary("text_transformer");
    }

    /**
     * Transforms every string in the input array to its uppercase equivalent.
     *
     * <p>Null elements within the array are preserved as null in the
     * result array — they are not skipped or replaced.</p>
     *
     * @param inputs Array of strings to transform; must not be {@code null}.
     * @return A new array of the same length containing the uppercased strings.
     * @throws IllegalArgumentException if {@code inputs} is {@code null}.
     */
    public native String[] toUpperCaseBatch(String[] inputs);
}