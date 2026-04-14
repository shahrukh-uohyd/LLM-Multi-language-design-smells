package com.example.jni;

/**
 * JNI bridge that starts a native long-running calculation.
 * The native code will call back into the provided handler during execution.
 */
public class NativeCalculationBridge {

    static {
        // Load the compiled shared library (libnativecalc.so / nativecalc.dll)
        System.loadLibrary("nativecalc");
    }

    /**
     * Starts the long-running native calculation.
     * This is a blocking call — run it on a background thread.
     *
     * @param handler The Java object that will receive periodic status updates.
     *                Must implement CalculationStatusHandler.
     * @return 0 on success, non-zero on failure.
     */
    public native int startCalculation(CalculationStatusHandler handler);


    // --- Example usage ---
    public static void main(String[] args) {
        NativeCalculationBridge bridge = new NativeCalculationBridge();
        CalculationStatusHandler handler = new DefaultCalculationHandler();

        // Run on a background thread since startCalculation is blocking
        Thread calcThread = new Thread(() -> {
            int result = bridge.startCalculation(handler);
            System.out.println("Native calculation finished with code: " + result);
        });

        calcThread.start();
    }
}