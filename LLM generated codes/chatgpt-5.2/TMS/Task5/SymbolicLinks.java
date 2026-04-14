package com.example.linux.fs.nativeops;

public final class SymbolicLinks {

    static {
        System.loadLibrary("linux_symlink_native");
    }

    private SymbolicLinks() {
        // utility class
    }

    /**
     * Creates a symbolic link.
     *
     * @param target existing file or directory
     * @param linkPath path of the symbolic link to create
     */
    public static native void createSymlink(String target, String linkPath);
}
