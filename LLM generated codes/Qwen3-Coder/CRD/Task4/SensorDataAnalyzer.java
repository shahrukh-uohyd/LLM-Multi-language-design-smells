// SensorDataAnalyzer.java
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class SensorDataAnalyzer {
    private DeviceController deviceController;
    private List<SensorDataObserver> observers;
    private boolean isRunning;

    public SensorDataAnalyzer() {
        this.deviceController = new DeviceController();
        this.observers = new ArrayList<>();
        this.isRunning = false;
    }

    public void addObserver(SensorDataObserver observer) {
        observers.add(observer);
    }

    public void removeObserver(SensorDataObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String event, Object data) {
        for (SensorDataObserver observer : observers) {
            observer.onSensorDataEvent(event, data);
        }
    }

    /**
     * Analyzes raw sensor data using signal processing techniques
     */
    public SignalAnalysisResult analyzeSensorData(double[] rawData) {
        try {
            notifyObservers("ANALYSIS_START", rawData);
            
            // Apply various signal processing techniques
            double[] filteredSignal = deviceController.lowPassFilter(rawData, 0.1);
            double[] windowedSignal = deviceController.hammingWindow(filteredSignal);
            double[] frequencyDomain = deviceController.fftTransform(windowedSignal);
            
            // Find peaks and calculate statistics
            double peakValue = deviceController.findPeakValue(windowedSignal);
            int peakIndex = deviceController.findPeakIndex(windowedSignal);
            double rms = deviceController.calculateRMS(windowedSignal);
            
            // Create analysis result
            SignalAnalysisResult result = new SignalAnalysisResult(
                filteredSignal,
                frequencyDomain,
                peakValue,
                peakIndex,
                rms
            );
            
            notifyObservers("ANALYSIS_COMPLETE", result);
            return result;
        } catch (Exception e) {
            System.err.println("Error analyzing sensor data: " + e.getMessage());
            notifyObservers("ANALYSIS_ERROR", e);
            return null;
        }
    }

    /**
     * Processes data from multiple sensors simultaneously
     */
    public List<SignalAnalysisResult> analyzeMultipleSensors(List<double[]> sensorDataList) {
        List<SignalAnalysisResult> results = new ArrayList<>();
        
        for (int i = 0; i < sensorDataList.size(); i++) {
            double[] sensorData = sensorDataList.get(i);
            SignalAnalysisResult result = analyzeSensorData(sensorData);
            if (result != null) {
                results.add(result);
            }
        }
        
        return results;
    }

    /**
     * Performs cross-correlation between two sensor signals
     */
    public double[] correlateSensorSignals(double[] signal1, double[] signal2) {
        try {
            return deviceController.correlateSignals(signal1, signal2);
        } catch (Exception e) {
            System.err.println("Error correlating sensor signals: " + e.getMessage());
            return null;
        }
    }

    /**
     * Real-time processing of streaming sensor data
     */
    public void startRealTimeProcessing(Consumer<double[]> dataProcessor) {
        isRunning = true;
        Thread processingThread = new Thread(() -> {
            while (isRunning) {
                // Simulate receiving data - in real app, this would come from actual sensors
                double[] simulatedData = generateSimulatedSensorData(100);
                
                CompletableFuture.runAsync(() -> {
                    SignalAnalysisResult result = analyzeSensorData(simulatedData);
                    if (result != null) {
                        dataProcessor.accept(result.processedSignal);
                    }
                });
                
                try {
                    Thread.sleep(100); // Process every 100ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        processingThread.setDaemon(true);
        processingThread.start();
    }

    /**
     * Stops real-time processing
     */
    public void stopRealTimeProcessing() {
        isRunning = false;
    }

    /**
     * Applies noise reduction to sensor data
     */
    public double[] reduceNoise(double[] noisySignal) {
        try {
            // Apply multiple filtering stages
            double[] stage1 = deviceController.highPassFilter(noisySignal, 0.01);
            double[] stage2 = deviceController.lowPassFilter(stage1, 0.2);
            double[] stage3 = deviceController.bandPassFilter(stage2, 0.05, 0.15);
            
            return stage3;
        } catch (Exception e) {
            System.err.println("Error reducing noise: " + e.getMessage());
            return noisySignal; // Return original if processing fails
        }
    }

    /**
     * Detects anomalies in sensor data
     */
    public AnomalyDetectionResult detectAnomalies(double[] signal) {
        try {
            double[] filteredSignal = deviceController.lowPassFilter(signal, 0.1);
            double mean = calculateMean(filteredSignal);
            double stdDev = calculateStandardDeviation(filteredSignal, mean);
            
            List<Integer> anomalyIndices = new ArrayList<>();
            for (int i = 0; i < filteredSignal.length; i++) {
                if (Math.abs(filteredSignal[i] - mean) > 2 * stdDev) {
                    anomalyIndices.add(i);
                }
            }
            
            return new AnomalyDetectionResult(anomalyIndices, mean, stdDev);
        } catch (Exception e) {
            System.err.println("Error detecting anomalies: " + e.getMessage());
            return new AnomalyDetectionResult(new ArrayList<>(), 0, 0);
        }
    }

    /**
     * Compares two sensor readings for similarity
     */
    public double calculateSimilarity(double[] signal1, double[] signal2) {
        try {
            // Cross-correlation gives similarity measure
            double[] correlation = deviceController.correlateSignals(signal1, signal2);
            if (correlation != null && correlation.length > 0) {
                // Return maximum correlation value as similarity measure
                double maxCorrelation = Double.NEGATIVE_INFINITY;
                for (double value : correlation) {
                    if (value > maxCorrelation) {
                        maxCorrelation = value;
                    }
                }
                return Math.abs(maxCorrelation);
            }
        } catch (Exception e) {
            System.err.println("Error calculating similarity: " + e.getMessage());
        }
        return 0.0;
    }

    private double[] generateSimulatedSensorData(int length) {
        double[] data = new double[length];
        for (int i = 0; i < length; i++) {
            // Simulate sine wave with noise
            data[i] = Math.sin(2 * Math.PI * i / 20.0) + 0.1 * Math.random();
        }
        return data;
    }

    private double calculateMean(double[] signal) {
        double sum = 0;
        for (double value : signal) {
            sum += value;
        }
        return sum / signal.length;
    }

    private double calculateStandardDeviation(double[] signal, double mean) {
        double sum = 0;
        for (double value : signal) {
            sum += Math.pow(value - mean, 2);
        }
        return Math.sqrt(sum / signal.length);
    }

    public static class SignalAnalysisResult {
        public final double[] processedSignal;
        public final double[] frequencyDomain;
        public final double peakValue;
        public final int peakIndex;
        public final double rms;

        public SignalAnalysisResult(double[] processedSignal, double[] frequencyDomain, 
                                  double peakValue, int peakIndex, double rms) {
            this.processedSignal = processedSignal;
            this.frequencyDomain = frequencyDomain;
            this.peakValue = peakValue;
            this.peakIndex = peakIndex;
            this.rms = rms;
        }
    }

    public static class AnomalyDetectionResult {
        public final List<Integer> anomalyIndices;
        public final double mean;
        public final double standardDeviation;

        public AnomalyDetectionResult(List<Integer> anomalyIndices, double mean, double standardDeviation) {
            this.anomalyIndices = anomalyIndices;
            this.mean = mean;
            this.standardDeviation = standardDeviation;
        }
    }
}