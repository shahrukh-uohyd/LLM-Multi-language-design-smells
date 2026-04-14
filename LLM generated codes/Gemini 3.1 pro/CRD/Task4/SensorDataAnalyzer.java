// The class responsible for high-level sensor data analysis
public class SensorDataAnalyzer {
    private final SignalProcessor signalProcessor;

    public SensorDataAnalyzer() {
        // Initialize the native signal processing dependency
        this.signalProcessor = new SignalProcessor();
    }

    public void analyzeVibrationData(float[] rawSensorReadings) {
        System.out.println("Received " + rawSensorReadings.length + " raw data points from sensor.");
        
        // Delegate noise reduction to native C++ implementation
        float smoothingFactor = 0.15f;
        float[] cleanSignal = signalProcessor.applyLowPassFilter(rawSensorReadings, smoothingFactor);
        
        System.out.println("Noise reduction complete. Proceeding with analysis...");
        
        // Delegate frequency extraction to native C++ implementation
        float[] frequencySpectrum = signalProcessor.computeFFT(cleanSignal);
        
        // TODO: Logic to analyze frequencySpectrum for anomalies
        System.out.println("Frequency spectrum computed successfully.");
    }
}