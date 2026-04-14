public class NativeConfigParser {

    static {
        System.loadLibrary("configparser");
    }

    /**
     * Parses configuration values from a string.
     *
     * @param config configuration text (e.g. "a=1;b=2")
     * @param keys keys to extract
     * @return parsed integer values aligned with keys[]
     */
    public static native int[] parseConfig(String config, String[] keys);
}
