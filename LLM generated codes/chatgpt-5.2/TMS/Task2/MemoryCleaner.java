package com.example.nativefeatures;

public final class MemoryCleaner {

    static {
        System.loadLibrary("memory_cleaner_native");
    }

    private MemoryCleaner() {
        // utility class
    }

    /**
     * Clears (zeroes) a block of native memory.
     *
     * @param address native memory address
     * @param size number of bytes to clear
     */
    public static native void clearMemory(long address, long size);
}
