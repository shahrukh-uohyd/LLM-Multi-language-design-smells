// DeviceController.java
import java.util.HashMap;
import java.util.Map;

public class DeviceController {
    static {
        System.loadLibrary("device_signal");
    }

    // Existing native device control methods
    public native boolean connectToDevice(String deviceId);
    public native boolean disconnectDevice(String deviceId);
    public native boolean sendCommand(String deviceId, String command);
    public native String readResponse(String deviceId);
    public native boolean configureDevice(String deviceId, String config);
    public native boolean isDeviceConnected(String deviceId);
    public native String getDeviceStatus(String deviceId);
    public native boolean resetDevice(String deviceId);

    // New signal processing native methods
    public native double[] filterSignal(double[] signal, String filterType, double parameter);
    public native double[] fftTransform(double[] signal);
    public native double[] correlateSignals(double[] signal1, double[] signal2);
    public native double findPeakValue(double[] signal);
    public native int findPeakIndex(double[] signal);
    public native double[] applyWindow(double[] signal, String windowType);
    public native double[] convolveSignals(double[] signal1, double[] signal2);
    public native double calculateRMS(double[] signal);
    public native double[] downsampleSignal(double[] signal, int factor);

    // Convenience methods for signal processing
    public double[] bandPassFilter(double[] signal, double lowFreq, double highFreq) {
        // This would be implemented with a more complex filter in native code
        return filterSignal(signal, "BANDPASS", (lowFreq + highFreq) / 2.0);
    }

    public double[] highPassFilter(double[] signal, double cutoffFreq) {
        return filterSignal(signal, "HIGHPASS", cutoffFreq);
    }

    public double[] lowPassFilter(double[] signal, double cutoffFreq) {
        return filterSignal(signal, "LOWPASS", cutoffFreq);
    }

    public double[] hammingWindow(double[] signal) {
        return applyWindow(signal, "HAMMING");
    }

    public double[] hanningWindow(double[] signal) {
        return applyWindow(signal, "HANNING");
    }

    public double[] blackmanWindow(double[] signal) {
        return applyWindow(signal, "BLACKMAN");
    }

    // Device-specific signal processing
    public double[] processSensorDataFromDevice(String deviceId, double[] rawSignal) {
        if (!isDeviceConnected(deviceId)) {
            throw new IllegalStateException("Device " + deviceId + " is not connected");
        }
        
        try {
            // Apply preprocessing filters
            double[] filteredSignal = lowPassFilter(rawSignal, 0.1);
            
            // Perform FFT analysis
            double[] frequencyDomain = fftTransform(filteredSignal);
            
            // Return processed data
            return frequencyDomain;
        } catch (Exception e) {
            System.err.println("Error processing sensor data from device " + deviceId + ": " + e.getMessage());
            return null;
        }
    }

    // Utility methods
    public static String formatSignal(double[] signal, int maxLength) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(signal.length, maxLength); i++) {
            sb.append(String.format("%.3f", signal[i]));
            if (i < Math.min(signal.length, maxLength) - 1) {
                sb.append(", ");
            }
        }
        if (signal.length > maxLength) {
            sb.append("...");
        }
        sb.append("]");
        return sb.toString();
    }
}