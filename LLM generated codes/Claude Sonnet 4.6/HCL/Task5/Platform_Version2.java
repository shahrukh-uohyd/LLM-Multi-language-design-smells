package com.audio.platform;

import java.util.Objects;

/**
 * Immutable snapshot of the host OS and CPU architecture.
 *
 * <p>Acts as the single authoritative source for all platform-specific
 * decisions made by the audio library loader, including resource-path
 * construction and native filename derivation.</p>
 *
 * <h2>Supported matrix</h2>
 * <pre>
 *  ┌──────────────┬──────────────┬───────────────────────────────────────┐
 *  │ OS           │ Architecture │ Resource path                         │
 *  ├──────────────┼──────────────┼───────────────────────────────────────┤
 *  │ Windows      │ x86_64       │ native/windows-x86_64/audiodecoder.dll│
 *  │ Linux        │ x86_64       │ native/linux-x86_64/libaudiodecoder.so│
 *  │ Linux        │ aarch64      │ native/linux-aarch64/libaudiodecoder.so│
 *  │ macOS        │ x86_64       │ native/macos-x86_64/libaudiodecoder.dylib│
 *  │ macOS        │ aarch64      │ native/macos-aarch64/libaudiodecoder.dylib│
 *  └──────────────┴──────────────┴───────────────────────────────────────┘
 * </pre>
 */
public final class Platform {

    private static final Platform CURRENT = new Platform(
            OperatingSystem.detect(),
            Architecture.detect()
    );

    private final OperatingSystem os;
    private final Architecture    arch;

    private Platform(OperatingSystem os, Architecture arch) {
        this.os   = Objects.requireNonNull(os);
        this.arch = Objects.requireNonNull(arch);
    }

    /** Returns the singleton {@link Platform} for the running JVM. */
    public static Platform current() {
        return CURRENT;
    }

    public OperatingSystem getOS()   { return os;   }
    public Architecture    getArch() { return arch; }

    /**
     * Compact platform tag used as the resource-directory name segment.
     * Example: {@code "linux-x86_64"}, {@code "macos-aarch64"}.
     */
    public String tag() {
        return os.name().toLowerCase(java.util.Locale.ROOT) + "-" + arch.getTag();
    }

    /**
     * Absolute classpath resource path for the named native library.
     *
     * @param baseName bare library name, e.g. {@code "audiodecoder"}
     * @return resource path suitable for {@link Class#getResourceAsStream(String)},
     *         e.g. {@code "/native/linux-x86_64/libaudiodecoder.so"}
     */
    public String resourcePath(String baseName) {
        return "/native/" + tag() + "/" + os.toFileName(baseName);
    }

    /**
     * Returns the OS-specific filename for the library (without directory).
     *
     * @param baseName bare library name, e.g. {@code "audiodecoder"}
     * @return e.g. {@code "audiodecoder.dll"} / {@code "libaudiodecoder.so"}
     */
    public String fileName(String baseName) {
        return os.toFileName(baseName);
    }

    @Override
    public String toString() {
        return "Platform{"
             + "os="           + os
             + ", arch="       + arch
             + ", tag='"       + tag()                                           + '\''
             + ", os.name='"   + System.getProperty("os.name",  "?")            + '\''
             + ", os.arch='"   + System.getProperty("os.arch",  "?")            + '\''
             + ", dataModel='" + System.getProperty("sun.arch.data.model", "?") + '\''
             + '}';
    }
}