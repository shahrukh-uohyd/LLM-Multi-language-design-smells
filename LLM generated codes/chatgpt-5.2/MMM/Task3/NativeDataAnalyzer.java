public class NativeDataAnalyzer {

    static {
        System.loadLibrary("dataanalyzer");
    }

    /**
     * Analyzes a numerical dataset.
     *
     * @param data input numerical values
     * @return double[] {sum, average, min, max}
     */
    public static native double[] analyze(double[] data);
}
