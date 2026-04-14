package com.example.linux.fs.nativeops;

public final class ExtendedAttributes {

    static {
        System.loadLibrary("linux_xattr_native");
    }

    private ExtendedAttributes() {
        // utility class
    }

    /**
     * Reads an extended attribute from a file.
     *
     * @param path file path
     * @param attributeName extended attribute name (e.g. "user.comment")
     * @return raw attribute value
     */
    public static native byte[] getAttribute(String path, String attributeName);
}
