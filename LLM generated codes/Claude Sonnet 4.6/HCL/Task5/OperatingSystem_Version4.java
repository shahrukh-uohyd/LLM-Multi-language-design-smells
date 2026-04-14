package com.audio.platform;

/**
 * Enumerates the operating systems supported by the audio processor.
 *
 * <p>Each constant carries the OS-specific native library prefix and
 * file extension so that {@link Platform} can derive the exact filename
 * without branching elsewhere in the codebase.</p>
 *
 * <ul>
 *   <li>Windows → {@code <name>.dll}            (no prefix)</li>
 *   <li>Linux   → {@code lib<name>.so}           (lib prefix)</li>
 *   <li>macOS   → {@code lib<name>.dylib}        (lib prefix)</li>
 * </ul>
 */
public enum OperatingSystem {

    WINDOWS("",    "dll"),
    LINUX  ("lib", "so"),
    MACOS  ("lib", "dylib");

    private final String libraryPrefix;
    private final String libraryExtension;

    OperatingSystem(String libraryPrefix, String libraryExtension) {
        this.libraryPrefix    = libraryPrefix;
        this.libraryExtension = libraryExtension;
    }

    /**
     * Constructs the OS-native filename for the given base library name.
     *
     * @param baseName bare library name without prefix or extension (e.g. {@code "audiodecoder"})
     * @return platform-specific filename  (e.g. {@code "libaudiodecoder.so"})
     */
    public String toFileName(String baseName) {
        return libraryPrefix + baseName + "." + libraryExtension;
    }

    /**
     * Detects the current OS from {@code os.name}.
     *
     * @return the matching {@link OperatingSystem}
     * @throws UnsupportedOperationException if the OS is not recognised
     */
    public static OperatingSystem detect() {
        String name = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);

        if (name.contains("win"))                               return WINDOWS;
        if (name.contains("nux") || name.contains("nix")
                || name.contains("aix"))                        return LINUX;
        if (name.contains("mac") || name.contains("darwin"))   return MACOS;

        throw new UnsupportedOperationException(
                "Unsupported operating system: \"" + name + "\". "
              + "Only Windows, Linux, and macOS are supported.");
    }
}