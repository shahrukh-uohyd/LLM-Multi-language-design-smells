package com.audio;

/**
 * Thrown when a native audio library operation fails — either during
 * library loading or during audio processing.
 *
 * <p>The message always includes actionable diagnostic information such
 * as the attempted resource path, platform tag, and {@code java.library.path}
 * where relevant.</p>
 */
public final class AudioException extends RuntimeException {

    public AudioException(String message) {
        super(message);
    }

    public AudioException(String message, Throwable cause) {
        super(message, cause);
    }
}