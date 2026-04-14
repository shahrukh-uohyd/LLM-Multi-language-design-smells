import java.util.Arrays;

public class SensorAnalyzer {

    private final DeviceController controller = new DeviceController();

    public double[] analyze(int[] rawSignal) {
        // Signal processing is mainly used here
        return controller.processSignal(rawSignal);
    }

    public static void main(String[] args) {

        SensorAnalyzer analyzer = new SensorAnalyzer();

        // Existing device control functionality
        analyzer.controller.performDeviceCheck();

        // Simulated sensor data
        int[] rawSignal = {10, 20, 30, 25, 15, 5};

        // New native signal processing feature
        double[] processed = analyzer.analyze(rawSignal);

        System.out.println("Processed signal:");
        System.out.println(Arrays.toString(processed));
    }
}
