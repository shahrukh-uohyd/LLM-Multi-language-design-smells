public class ConfigParser {

    // Load the native shared library
    static {
        System.loadLibrary("configparser");
    }

    /**
     * A structured class representing the parsed and usable configuration format.
     */
    public static class ConfigResult {
        public String host;
        public int port;
        public int timeout;
        public int maxRetries;
        public int maxConnections;

        // Constructor invoked by the C native code
        public ConfigResult(String host, int port, int timeout, int maxRetries, int maxConnections) {
            this.host = host;
            this.port = port;
            this.timeout = timeout;
            this.maxRetries = maxRetries;
            this.maxConnections = maxConnections;
        }

        @Override
        public String toString() {
            return String.format("Parsed Configuration:\n" +
                                 "  -> Host            : %s\n" +
                                 "  -> Port            : %d\n" +
                                 "  -> Timeout         : %d seconds\n" +
                                 "  -> Max Retries     : %d\n" +
                                 "  -> Max Connections : %d", 
                                 host, port, timeout, maxRetries, maxConnections);
        }
    }

    /**
     * Native method that receives configuration data (a String and a primitive array),
     * parses it, and returns a usable ConfigResult object.
     * 
     * @param rawConfig A semicolon-separated string of key=value pairs.
     * @param limits A primitive array containing [maxRetries, maxConnections].
     * @return The parsed ConfigResult object.
     */
    public native ConfigResult parseConfig(String rawConfig, int[] limits);

    public static void main(String[] args) {
        ConfigParser parser = new ConfigParser();

        // 1. Configuration values represented using strings and primitive arrays
        String rawConfigStr = "host=api.backend.local;port=8080;timeout=45";
        int[] configLimits = { 5, 250 }; // [0] = maxRetries, [1] = maxConnections

        System.out.println("--- JNI Configuration Parsing ---");
        System.out.println("Raw String Config : '" + rawConfigStr + "'");
        System.out.println("Raw Array Limits  : [" + configLimits[0] + ", " + configLimits[1] + "]\n");

        // 2. Pass to the native method for parsing
        ConfigResult result = parser.parseConfig(rawConfigStr, configLimits);

        // 3. The parsed output is returned to Java
        if (result != null) {
            System.out.println(result.toString());
        } else {
            System.err.println("Failed to parse configuration.");
        }
    }
}