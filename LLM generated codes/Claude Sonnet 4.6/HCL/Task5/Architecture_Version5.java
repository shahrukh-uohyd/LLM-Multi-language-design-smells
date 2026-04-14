package com.audio.platform;

/**
 * Enumerates the CPU architectures supported by the audio processor.
 *
 * <p>Detection uses two independent signals for maximum JVM-vendor coverage:</p>
 * <ol>
 *   <li>{@code sun.arch.data.model} — reliable on Oracle / OpenJDK ("32" or "64")</li>
 *   <li>{@code os.arch}             — raw arch string, used as fallback</li>
 * </ol>
 */
public enum Architecture {

    X86_64("x86_64"),
    AARCH64("aarch64"),
    X86_32("x86_32");

    private final String tag;

    Architecture(String tag) {
        this.tag = tag;
    }

    /** Returns the compact tag used in resource-path construction (e.g. {@code "x86_64"}). */
    public String getTag() {
        return tag;
    }

    /**
     * Detects the current CPU architecture from system properties.
     *
     * @return the matching {@link Architecture}
     * @throws UnsupportedOperationException if the architecture is not recognised
     */
    public static Architecture detect() {
        String dataModel = System.getProperty("sun.arch.data.model", "").trim();
        String osArch    = System.getProperty("os.arch",             "").toLowerCase(java.util.Locale.ROOT);

        // ARM 64-bit — must be checked before the generic "64" data-model branch
        if (osArch.equals("aarch64") || osArch.equals("arm64")) return AARCH64;

        // x86-64 via primary signal
        if ("64".equals(dataModel))     return X86_64;

        // x86-64 via fallback signal
        if (osArch.contains("64"))      return X86_64;

        // 32-bit via primary signal
        if ("32".equals(dataModel))     return X86_32;

        // 32-bit via fallback signal (x86, i386, i486, i586, i686)
        if (osArch.contains("86"))      return X86_32;

        throw new UnsupportedOperationException(
                "Unsupported CPU architecture: os.arch=\"" + osArch + "\", "
              + "sun.arch.data.model=\"" + dataModel + "\".");
    }
}