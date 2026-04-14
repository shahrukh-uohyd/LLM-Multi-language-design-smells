import java.util.Arrays;

public class DataAnalysisApp {

    public static void main(String[] args) {

        // Create numerical dataset
        double[] dataset = {
            10.5, 20.0, 15.2, 7.8, 30.1
        };

        // Call native analysis
        double[] result =
            NativeDataAnalyzer.analyze(dataset);

        System.out.println("Dataset: " + Arrays.toString(dataset));
        System.out.println("Sum: " + result[0]);
        System.out.println("Average: " + result[1]);
        System.out.println("Min: " + result[2]);
        System.out.println("Max: " + result[3]);
    }
}
