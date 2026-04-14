import java.util.Arrays;

/**
 * ConfigParser.java
 *
 * Java host class that:
 *  - Represents configuration data as strings and primitive arrays.
 *  - Declares five native parsing methods covering all major config shapes.
 *  - Passes each config payload to the native layer via JNI.
 *  - Receives, validates, and displays the structured results.
 *
 * Native parsing operations
 * ─────────────────────────
 *  ① parseKeyValue   – "key=value\nkey=value\n…" → parallel String[] / String[]
 *  ② parseIniSection – INI-style block → section name + key/value arrays
 *  ③ parseIntList    – "1,2,3,…" CSV of integers → int[]
 *  ④ parseDoubleList – "1.1,2.2,…" CSV of doubles → double[]
 *  ⑤ parseFlagBlock  – "FLAG_NAME:0|1\n…" → flag-name String[] + boolean[]
 */
public class ConfigParser {

    // ---------------------------------------------------------------
    // Result containers (plain Java value objects)
    // ---------------------------------------------------------------

    /** Holds a parsed key-value map as two parallel arrays. */
    public static class KeyValueResult {
        public final String[] keys;
        public final String[] values;
        public KeyValueResult(String[] keys, String[] values) {
            this.keys   = keys;
            this.values = values;
        }
    }

    /** Holds a parsed INI section: its name, keys, and values. */
    public static class IniSectionResult {
        public final String   sectionName;
        public final String[] keys;
        public final String[] values;
        public IniSectionResult(String sectionName, String[] keys, String[] values) {
            this.sectionName = sectionName;
            this.keys        = keys;
            this.values      = values;
        }
    }

    /** Holds parsed feature-flag names and their on/off states. */
    public static class FlagResult {
        public final String[]  names;
        public final boolean[] states;
        public FlagResult(String[] names, boolean[] states) {
            this.names  = names;
            this.states = states;
        }
    }

    // ---------------------------------------------------------------
    // Native method declarations
    // ---------------------------------------------------------------

    /**
     * Parses a newline-delimited "key=value" block.
     * Blank lines and lines starting with '#' are ignored.
     *
     * @param rawConfig the raw configuration string
     * @return KeyValueResult with parallel keys[] and values[] arrays
     */
    public native KeyValueResult parseKeyValue(String rawConfig);

    /**
     * Parses a single INI-style section block.
     *
     * Expected format:
     *   [SectionName]
     *   key1 = value1
     *   key2 = value2
     *
     * @param iniBlock one complete INI section (including the header line)
     * @return IniSectionResult with section name, keys[], and values[]
     */
    public native IniSectionResult parseIniSection(String iniBlock);

    /**
     * Parses a comma-separated list of integers.
     * Whitespace around commas is ignored.
     *
     * @param csvInts e.g. "1, 2, 3, 42"
     * @return int[] of parsed values
     */
    public native int[] parseIntList(String csvInts);

    /**
     * Parses a comma-separated list of doubles.
     * Whitespace around commas is ignored.
     *
     * @param csvDoubles e.g. "1.1, 2.2, 3.3"
     * @return double[] of parsed values
     */
    public native double[] parseDoubleList(String csvDoubles);

    /**
     * Parses a newline-delimited block of "FLAG_NAME:0|1" lines.
     * Lines starting with '#' are ignored.
     *
     * @param flagBlock the raw flags string
     * @return FlagResult with names[] and states[] arrays
     */
    public native FlagResult parseFlagBlock(String flagBlock);

    // ---------------------------------------------------------------
    // Static initialiser — load the shared library
    // ---------------------------------------------------------------
    static {
        System.loadLibrary("cfgparser");
    }

    // ---------------------------------------------------------------
    // Raw configuration payloads
    // ---------------------------------------------------------------

    private static final String KV_CONFIG =
        "# Application core settings\n" +
        "app.name       = MyApplication\n" +
        "app.version    = 2.5.1\n" +
        "app.env        = production\n" +
        "\n" +
        "# Server\n" +
        "server.host    = 192.168.1.10\n" +
        "server.port    = 8443\n" +
        "server.timeout = 30\n" +
        "\n" +
        "# Database\n" +
        "db.host        = db-primary.internal\n" +
        "db.port        = 5432\n" +
        "db.name        = appdb\n" +
        "db.pool.size   = 20\n";

    private static final String INI_SECTION =
        "[Cache]\n" +
        "host          = redis.internal\n" +
        "port          = 6379\n" +
        "ttl           = 3600\n" +
        "max_entries   = 10000\n" +
        "eviction      = lru\n";

    private static final String INT_CSV =
        "8080, 8081, 8082, 9090, 9091, 443, 80, 22";

    private static final String DOUBLE_CSV =
        "0.001, 1.5, 3.14159, 2.71828, 100.0, 0.9999, 42.42";

    private static final String FLAG_BLOCK =
        "# Feature flags\n" +
        "ENABLE_DARK_MODE:1\n" +
        "ENABLE_BETA_API:0\n" +
        "ALLOW_GUEST_ACCESS:1\n" +
        "USE_LEGACY_AUTH:0\n" +
        "ENABLE_RATE_LIMIT:1\n" +
        "MAINTENANCE_MODE:0\n" +
        "ENABLE_AUDIT_LOG:1\n";

    // ---------------------------------------------------------------
    // Display helpers
    // ---------------------------------------------------------------

    private static void printHeader(int idx, String title) {
        System.out.println(
            "\n┌──────────────────────────────────────────────────────────────");
        System.out.printf(
            "│ Parse Job #%d — %s%n", idx, title);
        System.out.println(
            "├──────────────────────────────────────────────────────────────");
    }

    private static void printKV(String[] keys, String[] values) {
        int w = 0;
        for (String k : keys) if (k.length() > w) w = k.length();
        String fmt = "│   %-" + w + "s  =  %s%n";
        for (int i = 0; i < keys.length; i++) {
            System.out.printf(fmt, keys[i], values[i]);
        }
    }

    // ---------------------------------------------------------------
    // Entry point
    // ---------------------------------------------------------------
    public static void main(String[] args) {

        ConfigParser parser = new ConfigParser();

        System.out.println(
            "╔══════════════════════════════════════════════════════════════╗");
        System.out.println(
            "║            JNI Configuration Parser Demo                    ║");
        System.out.println(
            "╚══════════════════════════════════════════════════════════════╝");

        // ── ① Key-Value block ──────────────────────────────────────────
        printHeader(1, "Key-Value Block");
        System.out.println("│ [RAW INPUT]");
        for (String line : KV_CONFIG.split("\n"))
            System.out.println("│   " + line);

        KeyValueResult kvr = parser.parseKeyValue(KV_CONFIG);
        System.out.println("│");
        System.out.printf ("│ [PARSED]  %d entries%n", kvr.keys.length);
        printKV(kvr.keys, kvr.values);

        // Spot-check
        String portVal = null;
        for (int i = 0; i < kvr.keys.length; i++)
            if ("server.port".equals(kvr.keys[i])) portVal = kvr.values[i];
        System.out.printf("│%n│ Spot-check server.port = \"%s\"  %s%n",
            portVal,
            "8443".equals(portVal) ? "✓ PASS" : "✗ FAIL");

        // ── ② INI section ──────────────────────────────────────────────
        printHeader(2, "INI Section Block");
        System.out.println("│ [RAW INPUT]");
        for (String line : INI_SECTION.split("\n"))
            System.out.println("│   " + line);

        IniSectionResult ini = parser.parseIniSection(INI_SECTION);
        System.out.println("│");
        System.out.printf ("│ [PARSED]  section=\"%s\",  %d entries%n",
                ini.sectionName, ini.keys.length);
        printKV(ini.keys, ini.values);
        System.out.printf("│%n│ Section name check: \"%s\"  %s%n",
            ini.sectionName,
            "Cache".equals(ini.sectionName) ? "✓ PASS" : "✗ FAIL");

        // ── ③ Integer CSV ──────────────────────────────────────────────
        printHeader(3, "Integer CSV");
        System.out.printf("│ [RAW INPUT]  \"%s\"%n", INT_CSV);

        int[] ints = parser.parseIntList(INT_CSV);
        System.out.println("│");
        System.out.println("│ [PARSED]");
        System.out.printf ("│   Values : %s%n", Arrays.toString(ints));
        System.out.printf ("│   Count  : %d  %s%n",
            ints.length, ints.length == 8 ? "✓ PASS" : "✗ FAIL");
        System.out.printf ("│   Sum    : %d%n",
            Arrays.stream(ints).sum());

        // ── ④ Double CSV ───────────────────────────────────────────────
        printHeader(4, "Double CSV");
        System.out.printf("│ [RAW INPUT]  \"%s\"%n", DOUBLE_CSV);

        double[] doubles = parser.parseDoubleList(DOUBLE_CSV);
        System.out.println("│");
        System.out.println("│ [PARSED]");
        System.out.printf ("│   Values : %s%n", Arrays.toString(doubles));
        System.out.printf ("│   Count  : %d  %s%n",
            doubles.length, doubles.length == 7 ? "✓ PASS" : "✗ FAIL");
        double dsum = 0; for (double v : doubles) dsum += v;
        System.out.printf ("│   Sum    : %.5f%n", dsum);

        // ── ⑤ Feature-flag block ───────────────────────────────────────
        printHeader(5, "Feature-Flag Block");
        System.out.println("│ [RAW INPUT]");
        for (String line : FLAG_BLOCK.split("\n"))
            System.out.println("│   " + line);

        FlagResult flags = parser.parseFlagBlock(FLAG_BLOCK);
        System.out.println("│");
        System.out.printf ("│ [PARSED]  %d flags%n", flags.names.length);
        int longestFlag = 0;
        for (String n : flags.names)
            if (n.length() > longestFlag) longestFlag = n.length();
        String flagFmt = "│   %-" + longestFlag + "s  →  %s%n";
        for (int i = 0; i < flags.names.length; i++)
            System.out.printf(flagFmt,
                flags.names[i], flags.states[i] ? "ON ✓" : "OFF ✗");

        // Spot-check ENABLE_DARK_MODE == true
        boolean darkMode = false;
        for (int i = 0; i < flags.names.length; i++)
            if ("ENABLE_DARK_MODE".equals(flags.names[i])) darkMode = flags.states[i];
        System.out.printf("│%n│ Spot-check ENABLE_DARK_MODE = %b  %s%n",
            darkMode, darkMode ? "✓ PASS" : "✗ FAIL");

        System.out.println(
            "\n└──────────────────────────────────────────────────────────────");
        System.out.println("All configuration blocks parsed successfully.");
    }
}