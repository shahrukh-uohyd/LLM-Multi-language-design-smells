package com.solver;

import java.util.Locale;

/**
 * Detects and encapsulates the host OS and CPU architecture.
 *
 * <p>Supported platform matrix:</p>
 * <pre>
 *  ┌──────────────┬─────────────┬──────────────────────────────┐
 *  │ OS           │ Architecture│ Resource path prefix          │
 *  ├──────────────┼─────────────┼──────────────────────────────┤
 *  │ Windows      │ x86_64      │ native/windows-x86_64/        │
 *  │ Linux        │ x86_64      │ native/linux-x86_64/          │
 *  │ Linux        │ aarch64     │ native/linux-aarch64/         │
 *  │ macOS        │ x86_64      │ native/macos-x86_64/          │
 *  │ macOS        │ aarch64     │ native/macos-aarch64/         │
 *  └──────────────┴─────────────┴──────────────────────────────┘
 * </pre>
 */
public final class Platform {

    // -------------------------------------------------------------------------
    // OS & arch detection (resolved once at class-load time)
    // -------------------------------------------------------------------------
    private static final String RAW_OS_NAME =
            System.getProperty("os.name",         "unknown").toLowerCase(Locale.ROOT);
    private static final String RAW_OS_ARCH =
            System.getProperty("os.arch",         "unknown").toLowerCase(Locale.ROOT);
    private static final String DATA_MODEL  =
            System.getProperty("sun.arch.data.model", "unknown");

    // -------------------------------------------------------------------------
    // Enums
    // -------------------------------------------------------------------------

    public enum OS {
        WINDOWS, LINUX, MACOS, UNKNOWN;
    }

    public enum Arch {
        X86_64, AARCH64, X86_32, UNKNOWN;
    }

    // -------------------------------------------------------------------------
    // Singleton state
    // -------------------------------------------------------------------------
    private final OS   os;
    private final Arch arch;

    private static final Platform INSTANCE = new Platform();

    private Platform() {
        this.os   = resolveOS();
        this.arch = resolveArch();
    }

    public static Platform current() {
        return INSTANCE;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public OS   getOS()   { return os;   }
    public Arch getArch() { return arch; }

    /**
     * Returns the platform-specific library file name, including prefix and extension.
     *
     * <p>Examples:
     * <ul>
     *   <li>Windows → {@code solver.dll}</li>
     *   <li>Linux   → {@code libsolver.so}</li>
     *   <li>macOS   → {@code libsolver.dylib}</li>
     * </ul>
     *
     * @param baseName bare library name (e.g. {@code "solver"})
     */
    public String nativeFileName(String baseName) {
        switch (os) {
            case WINDOWS: return baseName + ".dll";
            case MACOS:   return "lib" + baseName + ".dylib";
            case LINUX:   return "lib" + baseName + ".so";
            default:
                throw new UnsupportedOperationException("Unsupported OS: " + RAW_OS_NAME);
        }
    }

    /**
     * Returns the resource directory inside the JAR that contains the
     * native library for this platform (e.g. {@code native/linux-x86_64/}).
     */
    public String resourceDirectory() {
        return "native/" + platformTag() + "/";
    }

    /**
     * Returns the compact platform tag used in resource path and JAR packaging
     * (e.g. {@code linux-x86_64}, {@code windows-x86_64}).
     */
    public String platformTag() {
        String osTag;
        switch (os) {
            case WINDOWS: osTag = "windows"; break;
            case LINUX:   osTag = "linux";   break;
            case MACOS:   osTag = "macos";   break;
            default:
                throw new UnsupportedOperationException("Unsupported OS: " + RAW_OS_NAME);
        }
        String archTag;
        switch (arch) {
            case X86_64:  archTag = "x86_64";  break;
            case AARCH64: archTag = "aarch64"; break;
            case X86_32:  archTag = "x86_32";  break;
            default:
                throw new UnsupportedOperationException("Unsupported arch: " + RAW_OS_ARCH);
        }
        return osTag + "-" + archTag;
    }

    @Override
    public String toString() {
        return "Platform{os=" + os + ", arch=" + arch
             + ", rawOsName='" + RAW_OS_NAME + "'"
             + ", rawOsArch='" + RAW_OS_ARCH + "'"
             + ", dataModel='" + DATA_MODEL   + "'}";
    }

    // -------------------------------------------------------------------------
    // Internal resolvers
    // -------------------------------------------------------------------------

    private static OS resolveOS() {
        if (RAW_OS_NAME.contains("win"))                                  return OS.WINDOWS;
        if (RAW_OS_NAME.contains("nux") || RAW_OS_NAME.contains("nix")
                || RAW_OS_NAME.contains("aix"))                           return OS.LINUX;
        if (RAW_OS_NAME.contains("mac") || RAW_OS_NAME.contains("darwin")) return OS.MACOS;
        return OS.UNKNOWN;
    }

    private static Arch resolveArch() {
        // Primary signal — most reliable across JVM vendors
        if ("64".equals(DATA_MODEL)) {
            // Distinguish x86-64 from ARM-64
            if (RAW_OS_ARCH.contains("aarch64") || RAW_OS_ARCH.contains("arm64"))
                return Arch.AARCH64;
            return Arch.X86_64;
        }
        if ("32".equals(DATA_MODEL)) return Arch.X86_32;

        // Fallback signal — raw os.arch value
        if (RAW_OS_ARCH.equals("aarch64") || RAW_OS_ARCH.equals("arm64")) return Arch.AARCH64;
        if (RAW_OS_ARCH.contains("64"))                                    return Arch.X86_64;
        if (RAW_OS_ARCH.contains("86"))                                    return Arch.X86_32;
        return Arch.UNKNOWN;
    }
}