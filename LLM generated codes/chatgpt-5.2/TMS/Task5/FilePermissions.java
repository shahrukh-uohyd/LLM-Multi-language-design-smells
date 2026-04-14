package com.example.linux.fs.nativeops;

public final class FilePermissions {

    static {
        System.loadLibrary("linux_chmod_native");
    }

    private FilePermissions() {
        // utility class
    }

    /**
     * Changes file permissions.
     *
     * @param path file path
     * @param mode permission bits (e.g. 0644, 0755)
     */
    public static native void chmod(String path, int mode);
}
