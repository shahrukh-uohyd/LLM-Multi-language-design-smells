package com.example;

/**
 * Immutable container for statistical analysis results.
 */
public final class StatisticsResult {
    public final double mean;
    public final double median;
    public final double standardDeviation;
    public final double minimum;
    public final double maximum;
    public final int validCount;
    public final int nanCount;
    
    public StatisticsResult(double mean, double median, double standardDeviation, 
                           double minimum, double maximum, int validCount, int nanCount) {
        this.mean = mean;
        this.median = median;
        this.standardDeviation = standardDeviation;
        this.minimum = minimum;
        this.maximum = maximum;
        this.validCount = validCount;
        this.nanCount = nanCount;
    }
    
    @Override
    public String toString() {
        return String.format(
            "StatisticsResult{mean=%.4f, median=%.4f, stdDev=%.4f, min=%.4f, max=%.4f, " +
            "valid=%d, nan=%d}",
            mean, median, standardDeviation, minimum, maximum, validCount, nanCount
        );
    }
}