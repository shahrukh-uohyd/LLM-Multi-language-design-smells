package com.example.imageservice.model;

/**
 * Enumerates all image formats the service is capable of handling.
 * The ordinal value is passed across the JNI boundary; keep the order stable.
 */
public enum ImageFormat {
    UNKNOWN,   // 0 – unrecognised / not yet detected
    JPEG,      // 1
    PNG,       // 2
    WEBP,      // 3
    GIF,       // 4
    BMP        // 5
}