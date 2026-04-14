public class NumericalAnalyzer {

    // Load the native shared library
    static {
        System.loadLibrary("numericalanalyzer");
    }

    /**
     * Native method that receives an array of numerical values, performs statistical
     * analysis, and returns the results.
     * 
     * @param dataset The numerical dataset (double array).
     * @return A double array containing the results in the following order:
     *         [0] = Minimum
     *         [1] = Maximum
     *         [2] = Sum
     *         [3] = Average
     *         [4] = Population Variance
     */
    public native double[] analyze(double[] dataset);

    public static void main(String[] args) {
        NumericalAnalyzer analyzer = new NumericalAnalyzer();

        // 1. Create arrays of numerical values
        double[] dataset1 = { 12.5, 45.2, 8.9, 99.1, 23.4, 55.0, 7.3, 31.8 };
        double[] dataset2 = { -10.0, 0.0, 10.0, 20.0, -20.0 };

        System.out.println("--- JNI Numerical Dataset Analysis ---\n");
        
        processAndPrint(analyzer, "Dataset 1", dataset1);
        processAndPrint(analyzer, "Dataset 2", dataset2);
    }

    private static void processAndPrint(NumericalAnalyzer analyzer, String name, double[] dataset) {
        System.out.print(name + " Data: [ ");
        for (double val : dataset) {
            System.out.print(val + " ");
        }
        System.out.println("]");

        // 2. Pass the dataset to the native method to get the analysis result
        double[] results = analyzer.analyze(dataset);

        if (results != null && results.length == 5) {
            System.out.printf("  -> Minimum  : %.4f\n", results[0]);
            System.out.printf("  -> Maximum  : %.4f\n", results[1]);
            System.out.printf("  -> Sum      : %.4f\n", results[2]);
            System.out.printf("  -> Average  : %.4f\n", results[3]);
            System.out.printf("  -> Variance : %.4f\n\n", results[4]);
        } else {
            System.out.println("  -> Error: Analysis failed or dataset was empty.\n");
        }
    }
}