/**
 * TextNormalizer.java
 *
 * Java host class that:
 *  - Declares a native normalization method.
 *  - Prepares a set of raw textual values.
 *  - Invokes the native method for each value and prints the result.
 */
public class TextNormalizer {

    // ---------------------------------------------------------------
    // Native method declaration
    // ---------------------------------------------------------------

    /**
     * Normalizes a single textual value via JNI.
     *
     * The native side performs:
     *   1. Leading / trailing whitespace trimming.
     *   2. Lower-case transformation (ASCII-safe, locale-independent).
     *   3. Collapse of internal runs of whitespace to a single space.
     *   4. UTF-8 encoding validation / replacement of lone surrogates.
     *
     * @param rawText the raw, possibly dirty string to normalize
     * @return the normalized string, never null
     */
    public native String normalizeText(String rawText);

    // ---------------------------------------------------------------
    // Static initializer — load the shared library
    // ---------------------------------------------------------------
    static {
        /*
         * The library is expected to reside in the directory pointed to by
         * -Djava.library.path when the JVM is launched.
         *
         * On Linux  : libtextnorm.so
         * On macOS  : libtextnorm.dylib
         * On Windows: textnorm.dll
         */
        System.loadLibrary("textnorm");
    }

    // ---------------------------------------------------------------
    // Entry point
    // ---------------------------------------------------------------
    public static void main(String[] args) {

        TextNormalizer normalizer = new TextNormalizer();

        // A representative set of "dirty" input values
        String[] rawInputs = {
            "  Hello, World!  ",                      // leading/trailing spaces
            "\t\tJAVA  JNI   DEMO\n",                  // tabs, newlines, mixed case
            "   Normalize   ME   ",                    // internal multi-spaces
            "UPPERCASE INPUT",                         // all-caps
            "already clean",                           // already normalized
            "  MiXeD   CaSe   AnD   SpAcEs  ",         // mixed case + spaces
            "\u00C9l\u00E8ve en fran\u00E7ais",        // accented characters (UTF-8)
            "  \u4e2d\u6587\u6d4b\u8bd5  ",            // Chinese characters
            "",                                        // empty string
            "   ",                                     // only whitespace
        };

        System.out.println("=== JNI Text Normalizer ===\n");
        System.out.printf("%-5s | %-40s | %s%n", "No.", "Raw Input", "Normalized Output");
        System.out.println("-".repeat(80));

        for (int i = 0; i < rawInputs.length; i++) {
            String raw        = rawInputs[i];
            String normalized = normalizer.normalizeText(raw);

            // Display control characters visually
            String displayRaw = raw.replace("\t", "\\t")
                                   .replace("\n", "\\n")
                                   .replace("\r", "\\r");

            System.out.printf("%-5d | %-40s | \"%s\"%n",
                              i + 1, "\"" + displayRaw + "\"", normalized);
        }

        System.out.println("\nDone.");
    }
}