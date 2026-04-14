package com.app.platform;

/**
 * Provides platform-specific utility operations through native methods.
 * This class is NOT modified — shown here only as architectural context.
 */
public class PlatformUtils {

    static {
        System.loadLibrary("platform_utils"); // existing native library
    }

    // --- existing native methods (unchanged) ---
    public native String getPlatformName();
    public native long getAvailableMemory();
    public native boolean isLittleEndian();
    public native int getProcessorCount();
}