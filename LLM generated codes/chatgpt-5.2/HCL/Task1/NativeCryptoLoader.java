package com.example.crypto;

public final class NativeCryptoLoader {

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();

    private static final String WINDOWS_LIB = "crypto-native";
    private static final String LINUX_LIB   = "crypto-linux"; // without lib prefix

    private static volatile boolean loaded = false;

    private NativeCryptoLoader() {
        // utility class – no instances
    }

    /**
     * Loads the native crypto library appropriate for the current OS.
     * This method is safe to call multiple times.
     */
    public static synchronized void load() {
        if (loaded) {
            return;
        }

        try {
            if (isWindows()) {
                System.loadLibrary(WINDOWS_LIB);
            } else if (isLinux()) {
                System.loadLibrary(LINUX_LIB);
            } else {
                throw new UnsupportedOperationException(
                        "Unsupported operating system: " + OS_NAME
                );
            }

            loaded = true;

        } catch (UnsatisfiedLinkError e) {
            throw new IllegalStateException(
                    "Failed to load native crypto library for OS: " + OS_NAME,
                    e
            );
        }
    }

    private static boolean isWindows() {
        return OS_NAME.contains("win");
    }

    private static boolean isLinux() {
        return OS_NAME.contains("linux");
    }
}
