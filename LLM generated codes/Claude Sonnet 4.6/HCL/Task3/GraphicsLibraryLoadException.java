/**
 * Thrown when every candidate native graphics library name has failed to load.
 *
 * <p>The exception message contains a full per-candidate diagnostic report
 * so developers can immediately understand what was tried, what the JVM
 * search path was, and what specific linker error occurred for each name.</p>
 */
public final class GraphicsLibraryLoadException extends RuntimeException {

    private final LoadAttempt[] attempts;

    public GraphicsLibraryLoadException(
            LoadAttempt[] attempts,
            String osName,
            String osArch,
            String javaLibraryPath) {

        super(buildMessage(attempts, osName, osArch, javaLibraryPath));
        this.attempts = attempts.clone();
    }

    /** Returns a copy of all load attempts for programmatic inspection. */
    public LoadAttempt[] getAttempts() {
        return attempts.clone();
    }

    // -----------------------------------------------------------------------
    // Diagnostic message builder
    // -----------------------------------------------------------------------
    private static String buildMessage(
            LoadAttempt[] attempts,
            String osName,
            String osArch,
            String javaLibraryPath) {

        StringBuilder sb = new StringBuilder();
        sb.append("\n╔══════════════════════════════════════════════════════════╗\n");
        sb.append(  "║       FATAL – Native Graphics Library Load Failure       ║\n");
        sb.append(  "╚══════════════════════════════════════════════════════════╝\n");
        sb.append("Environment:\n");
        sb.append("  OS name          : ").append(osName).append('\n');
        sb.append("  OS arch          : ").append(osArch).append('\n');
        sb.append("  java.library.path: ").append(javaLibraryPath).append('\n');
        sb.append("\nAttempted candidates (").append(attempts.length).append("):\n");

        for (int i = 0; i < attempts.length; i++) {
            sb.append(String.format("  [%d] %s%n", i + 1, attempts[i]));
        }

        sb.append("\nResolution checklist:\n");
        sb.append("  1. Verify the library file exists in one of the java.library.path directories.\n");
        sb.append("  2. On Linux, System.loadLibrary(\"foo\") looks for 'libfoo.so'.\n");
        sb.append("     If your file is 'libgraphics_core.so', add 'graphics_core' as a candidate.\n");
        sb.append("  3. Confirm the library was compiled for this OS and architecture.\n");
        sb.append("  4. Check for missing transitive dependencies (e.g. with 'ldd' on Linux,\n");
        sb.append("     'otool -L' on macOS, or 'Dependency Walker' on Windows).\n");
        sb.append("  5. Run with -Djava.library.path=/absolute/path/to/native/libs\n");

        return sb.toString();
    }
}