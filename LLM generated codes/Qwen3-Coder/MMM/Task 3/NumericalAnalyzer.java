package com.example;

/**
 * High-performance numerical dataset analyzer using JNI.
 * Supports statistical analysis, outlier detection, normalization, and histogram generation.
 */
public class NumericalAnalyzer {
    
    // Load native library with comprehensive fallback strategy
    static {
        boolean loaded = false;
        String[] libNames = {
            "numerical_analyzer",
            "libnumerical_analyzer.so",
            "numerical_analyzer.dll",
            "libnumerical_analyzer.dylib"
        };
        
        for (String lib : libNames) {
            try {
                String cleanName = lib
                    .replace("lib", "")
                    .replace(".so", "")
                    .replace(".dylib", "")
                    .replace(".dll", "");
                System.loadLibrary(cleanName);
                loaded = true;
                break;
            } catch (UnsatisfiedLinkError e1) {
                try {
                    System.load("./lib/" + lib);
                    loaded = true;
                    break;
                } catch (UnsatisfiedLinkError e2) {
                    // Continue trying other options
                }
            }
        }
        
        if (!loaded) {
            throw new RuntimeException(
                "Failed to load native library. Ensure libnumerical_analyzer is in java.library.path\n" +
                "Current path: " + System.getProperty("java.library.path")
            );
        }
    }

    // ===== NATIVE METHODS =====
    
    /**
     * Calculate comprehensive statistics: mean, median, std dev, min, max
     * @param data Input numerical dataset
     * @return StatisticsResult object containing all metrics
     * @throws IllegalArgumentException for invalid inputs
     */
    public native StatisticsResult calculateStatistics(double[] data);
    
    /**
     * Find minimum and maximum values with their indices
     * @param data Input numerical dataset
     * @return double[4] = {minValue, minIndex, maxValue, maxIndex}
     */
    public native double[] findMinMax(double[] data);
    
    /**
     * Generate histogram with specified number of bins
     * @param data Input numerical dataset
     * @param numBins Number of histogram bins (must be > 0)
     * @return Histogram object containing bin counts and edges
     */
    public native Histogram generateHistogram(double[] data, int numBins);
    
    /**
     * Detect outliers using Interquartile Range (IQR) method
     * Outliers defined as values < Q1 - 1.5*IQR or > Q3 + 1.5*IQR
     * @param data Input numerical dataset
     * @return boolean[] where true indicates an outlier at that index
     */
    public native boolean[] detectOutliers(double[] data);
    
    /**
     * Normalize data using Z-score transformation: (x - mean) / stdDev
     * @param data Input numerical dataset
     * @return Normalized dataset with mean=0, stdDev=1
     */
    public native double[] normalizeZScore(double[] data);
    
    /**
     * Calculate moving average with specified window size
     * @param data Input numerical dataset
     * @param windowSize Size of moving average window (must be > 0)
     * @return Smoothed dataset (length = data.length - windowSize + 1)
     */
    public native double[] calculateMovingAverage(double[] data, int windowSize);

    // ===== JAVA FALLBACK IMPLEMENTATIONS =====
    
    public StatisticsResult calculateStatisticsJava(double[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data array cannot be null or empty");
        }
        
        // Mean
        double sum = 0.0;
        int validCount = 0;
        for (double v : data) {
            if (!Double.isNaN(v)) {
                sum += v;
                validCount++;
            }
        }
        if (validCount == 0) throw new IllegalArgumentException("No valid values in dataset");
        double mean = sum / validCount;
        
        // Standard deviation
        double sumSq = 0.0;
        for (double v : data) {
            if (!Double.isNaN(v)) {
                double diff = v - mean;
                sumSq += diff * diff;
            }
        }
        double stdDev = Math.sqrt(sumSq / validCount);
        
        // Min/Max (excluding NaN)
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (double v : data) {
            if (!Double.isNaN(v)) {
                if (v < min) min = v;
                if (v > max) max = v;
            }
        }
        
        // Median (requires sorted copy)
        double[] sorted = new double[validCount];
        int idx = 0;
        for (double v : data) {
            if (!Double.isNaN(v)) sorted[idx++] = v;
        }
        java.util.Arrays.sort(sorted);
        double median = (validCount % 2 == 1) 
            ? sorted[validCount / 2] 
            : (sorted[(validCount - 1) / 2] + sorted[validCount / 2]) / 2.0;
        
        return new StatisticsResult(mean, median, stdDev, min, max, validCount, data.length - validCount);
    }
}