package com.example;

/**
 * Immutable container for histogram data.
 */
public final class Histogram {
    public final double[] binEdges;   // Length = numBins + 1
    public final int[] binCounts;     // Length = numBins
    
    public Histogram(double[] binEdges, int[] binCounts) {
        if (binEdges == null || binCounts == null || binEdges.length != binCounts.length + 1) {
            throw new IllegalArgumentException("Invalid histogram dimensions");
        }
        this.binEdges = binEdges.clone();
        this.binCounts = binCounts.clone();
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Histogram{\n");
        for (int i = 0; i < binCounts.length; i++) {
            sb.append(String.format("  [%.2f, %.2f): %d\n", 
                binEdges[i], binEdges[i + 1], binCounts[i]));
        }
        sb.append("}");
        return sb.toString();
    }
}