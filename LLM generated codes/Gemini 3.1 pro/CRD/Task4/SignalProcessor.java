// New utility class specifically for signal processing JNI calls
public class SignalProcessor {
    static {
        // Loads the new native library for signal processing (e.g., libsignalprocessing.so)
        System.loadLibrary("signalprocessing");
    }

    // Native method to apply a low-pass filter to a raw sensor signal
    public native float[] applyLowPassFilter(float[] rawSignal, float smoothingFactor);

    // Native method to compute the Fast Fourier Transform (FFT) of a signal
    public native float[] computeFFT(float[] timeDomainSignal);
}