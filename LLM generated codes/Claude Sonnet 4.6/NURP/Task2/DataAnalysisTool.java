package com.dataanalysis;

import com.dataanalysis.init.NativeLibraryLoader;

/**
 * Entry point for the portable data analysis tool.
 */
public class DataAnalysisTool {

    static {
        // Load the native data_helper library before any native methods are invoked.
        // This runs exactly once, regardless of how many times this class is loaded.
        NativeLibraryLoader.load();
    }

    // Declare native methods backed by data_helper after load() has run.
    public native double[] analyzeDataSet(double[] input);
    public native String   getSummaryReport(long dataSetId);
    public native void     releaseResources();

    public static void main(String[] args) {
        DataAnalysisTool tool = new DataAnalysisTool();
        // Native methods are now safe to call.
    }
}