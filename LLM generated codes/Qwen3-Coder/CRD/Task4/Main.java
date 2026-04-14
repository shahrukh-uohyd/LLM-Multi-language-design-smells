// Main.java - Example usage
public class Main {
    public static void main(String[] args) {
        DeviceController controller = new DeviceController();
        SensorDataAnalyzer analyzer = new Analyzer();
        
        // Connect to a device
        String deviceId = "SENSOR_001";
        boolean connected = controller.connectToDevice(deviceId);
        System.out.println("Device connected: " + connected);
        
        // Check device status
        System.out.println("Device status: " + controller.getDeviceStatus(deviceId));
        
        // Generate some sample sensor data
        double[] sampleData = new double[100];
        for (int i = 0; i < sampleData.length; i++) {
            // Sine wave with some noise
            sampleData[i] = Math.sin(2 * Math.PI * i / 20.0) + 0.1 * Math.random();
        }
        
        System.out.println("Sample signal: " + DeviceController.formatSignal(sampleData, 10));
        
        // Test signal processing
        System.out.println("\n=== Testing Signal Processing ===");
        
        // Low pass filter
        double[] filteredSignal = controller.lowPassFilter(sampleData, 0.1);
        System.out.println("Filtered signal: " + DeviceController.formatSignal(filteredSignal, 10));
        
        // FFT transform
        double[] frequencyDomain = controller.fftTransform(filteredSignal);
        System.out.println("FFT result (first 10): " + DeviceController.formatSignal(frequencyDomain, 10));
        
        // Find peak
        double peakValue = controller.findPeakValue(filteredSignal);
        int peakIndex = controller.findPeakIndex(filteredSignal);
        System.out.println("Peak value: " + peakValue + " at index: " + peakIndex);
        
        // RMS calculation
        double rms = controller.calculateRMS(filteredSignal);
        System.out.println("RMS value: " + rms);
        
        // Windowing
        double[] windowedSignal = controller.hammingWindow(filteredSignal);
        System.out.println("Windowed signal (first 10): " + DeviceController.formatSignal(windowedSignal, 10));
        
        // Test sensor data analysis
        System.out.println("\n=== Testing Sensor Data Analysis ===");
        
        SensorDataAnalyzer.SignalAnalysisResult analysisResult = analyzer.analyzeSensorData(sampleData);
        if (analysisResult != null) {
            System.out.println("Analysis completed:");
            System.out.println("  Peak value: " + analysisResult.peakValue);
            System.out.println("  Peak index: " + analysisResult.peakIndex);
            System.out.println("  RMS: " + analysisResult.rms);
            System.out.println("  Processed signal length: " + analysisResult.processedSignal.length);
        }
        
        // Test noise reduction
        System.out.println("\n=== Testing Noise Reduction ===");
        double[] noisySignal = new double[sampleData.length];
        for (int i = 0; i < sampleData.length; i++) {
            noisySignal[i] = sampleData[i] + 0.2 * (Math.random() - 0.5); // Add more noise
        }
        
        double[] reducedNoise = analyzer.reduceNoise(noisySignal);
        double originalRMS = controller.calculateRMS(noisySignal);
        double reducedRMS = controller.calculateRMS(reducedNoise);
        System.out.println("Original signal RMS: " + originalRMS);
        System.out.println("Reduced noise RMS: " + reducedRMS);
        System.out.println("Noise reduction: " + (originalRMS - reducedRMS) / originalRMS * 100 + "%");
        
        // Test anomaly detection
        System.out.println("\n=== Testing Anomaly Detection ===");
        double[] signalWithAnomaly = new double[sampleData.length];
        System.arraycopy(sampleData, 0, signalWithAnomaly, 0, sampleData.length);
        signalWithAnomaly[50] = 5.0; // Inject anomaly
        
        SensorDataAnalyzer.AnomalyDetectionResult anomalyResult = analyzer.detectAnomalies(signalWithAnomaly);
        System.out.println("Detected anomalies at indices: " + anomalyResult.anomalyIndices);
        System.out.println("Signal mean: " + anomalyResult.mean);
        System.out.println("Signal std dev: " + anomalyResult.standardDeviation);
        
        // Test cross-correlation
        System.out.println("\n=== Testing Cross-Correlation ===");
        double[] signal1 = {1, 2, 3, 4, 5};
        double[] signal2 = {5, 4, 3, 2, 1};
        double[] correlation = analyzer.correlateSensorSignals(signal1, signal2);
        System.out.println("Cross-correlation result: " + DeviceController.formatSignal(correlation, 10));
        
        // Similarity calculation
        double similarity = analyzer.calculateSimilarity(signal1, signal2);
        System.out.println("Signal similarity: " + similarity);
        
        // Disconnect device
        controller.disconnectDevice(deviceId);
        System.out.println("Device disconnected. Status: " + controller.getDeviceStatus(deviceId));
    }
}