package com.example.nativefeatures;

public final class FileChecksum {

    static {
        System.loadLibrary("file_checksum_native");
    }

    private FileChecksum() {
        // utility class
    }

    /**
     * Computes a checksum for the given file using native code.
     *
     * @param filePath absolute or relative file path
     * @return checksum bytes (algorithm defined in native layer)
     */
    public static native byte[] computeChecksum(String filePath);
}
