package com.example.imageservice.model;

import java.util.Objects;

/**
 * Carries the raw bytes of a single uploaded image together with
 * the file name supplied by the client.
 */
public final class UploadedImage {

    private final String filename;   // original filename from the upload
    private final byte[] data;       // raw file bytes

    public UploadedImage(String filename, byte[] data) {
        this.filename = Objects.requireNonNull(filename, "filename");
        this.data     = Objects.requireNonNull(data,     "data");
    }

    public String getFilename() { return filename; }
    public byte[] getData()     { return data; }
    public int    getSize()     { return data.length; }
}