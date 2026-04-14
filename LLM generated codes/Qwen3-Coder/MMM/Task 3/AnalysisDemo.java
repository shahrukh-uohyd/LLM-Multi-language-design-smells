package com.example;

import java.util.Random;

/**
 * Comprehensive demonstration of numerical analysis capabilities.
 */
public class AnalysisDemo {
    
    public static void main(String[] args) {
        NumericalAnalyzer analyzer = new NumericalAnalyzer();
        Random random = new Random(12345); // Deterministic for demo
        
        System.out.println("=== JNI Numerical Dataset Analyzer ===\n");
        
        // Test 1: Basic statistics on normal distribution
        System.out.println("Test 1: Statistics on Normal Distribution (μ=50, σ=15, n=1000)");
        double[] normalData = generateNormalData(random, 1000, 50.0, 15.0);
        StatisticsResult stats = analyzer.calculateStatistics(normalData);
        System.out.println(stats + "\n");
        
        // Test 2: Min/Max with indices
        System.out.println("Test 2: Min/Max Detection");
        double[] minMaxData = {3.5, 7.2, 1.8, 9.4, 4.1, Double.NaN, 2.7};
        double[] minMax = analyzer.findMinMax(minMaxData);
        System.out.printf("Min: %.2f at index %.0f | Max: %.2f at index %.0f\n\n", 
            minMax[0], minMax[1], minMax[2], minMax[3]);
        
        // Test 3: Histogram generation
        System.out.println("Test 3: Histogram (10 bins)");
        Histogram hist = analyzer.generateHistogram(normalData, 10);
        System.out.println(hist + "\n");
        
        // Test 4: Outlier detection
        System.out.println("Test 4: Outlier Detection (IQR Method)");
        double[] withOutliers = generateNormalData(random, 200, 100.0, 10.0);
        withOutliers[195] = 180.0;  // Introduce high outlier
        withOutliers[196] = 185.0;  // Introduce high outlier
        withOutliers[197] = 25.0;   // Introduce low outlier
        boolean[] outliers = analyzer.detectOutliers(withOutliers);
        int outlierCount = 0;
        for (boolean isOutlier : outliers) if (isOutlier) outlierCount++;
        System.out.printf("Detected %d outliers in dataset of size %d\n\n", 
            outlierCount, withOutliers.length);
        
        // Test 5: Z-score normalization
        System.out.println("Test 5: Z-Score Normalization");
        double[] normalized = analyzer.normalizeZScore(normalData);
        StatisticsResult normStats = analyzer.calculateStatistics(normalized);
        System.out.println("Original stats: " + stats);
        System.out.println("Normalized stats: " + normStats + "\n");
        
        // Test 6: Moving average smoothing
        System.out.println("Test 6: Moving Average Smoothing (window=5)");
        double[] noisyData = generateNoisySineWave(random, 50, 10.0, 2.0);
        double[] smoothed = analyzer.calculateMovingAverage(noisyData, 5);
        System.out.printf("Original length: %d | Smoothed length: %d\n\n", 
            noisyData.length, smoothed.length);
        
        // Test 7: Edge cases
        System.out.println("Test 7: Edge Case Handling");
        testEdgeCases(analyzer);
        
        System.out.println("=== Analysis Complete ===");
    }
    
    private static double[] generateNormalData(Random random, int size, double mean, double stdDev) {
        double[] data = new double[size];
        for (int i = 0; i < size; i++) {
            // Box-Muller transform for normal distribution
            double u1 = 1.0 - random.nextDouble();
            double u2 = random.nextDouble();
            double z0 = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2 * Math.PI * u2);
            data[i] = mean + z0 * stdDev;
        }
        return data;
    }
    
    private static double[] generateNoisySineWave(Random random, int size, double amplitude, double noiseLevel) {
        double[] data = new double[size];
        for (int i = 0; i < size; i++) {
            double x = 2 * Math.PI * i / size;
            data[i] = amplitude * Math.sin(x) + (random.nextDouble() * 2 - 1) * noiseLevel;
        }
        return data;
    }
    
    private static void testEdgeCases(NumericalAnalyzer analyzer) {
        // Empty array
        try {
            analyzer.calculateStatistics(new double[0]);
            System.out.println("Empty array: FAILED (should throw exception)");
        } catch (IllegalArgumentException e) {
            System.out.println("Empty array: PASSED (correctly rejected)");
        }
        
        // All NaN values
        try {
            double[] allNaN = {Double.NaN, Double.NaN, Double.NaN};
            analyzer.calculateStatistics(allNaN);
            System.out.println("All NaN: FAILED (should throw exception)");
        } catch (IllegalArgumentException e) {
            System.out.println("All NaN: PASSED (correctly rejected)");
        }
        
        // Single value
        double[] single = {42.0};
        StatisticsResult singleStats = analyzer.calculateStatistics(single);
        System.out.printf("Single value stats: mean=%.1f, stdDev=%.1f\n", 
            singleStats.mean, singleStats.standardDeviation);
        
        // Mixed valid/NaN values
        double[] mixed = {1.0, 2.0, Double.NaN, 3.0, Double.NaN, 4.0};
        StatisticsResult mixedStats = analyzer.calculateStatistics(mixed);
        System.out.printf("Mixed values: valid=%d, nan=%d, mean=%.2f\n", 
            mixedStats.validCount, mixedStats.nanCount, mixedStats.mean);
    }
}