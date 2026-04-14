public class HeartRateMonitor {

    static {
        System.loadLibrary("wearable_health_native");
    }

    // Native declarations for sensor interaction and DSP (Digital Signal Processing)
    private native byte[] readPpgSensor();
    private native int calculateBpm(byte[] ppgData);

    /**
     * Captures raw optical data from the PPG sensor and calculates the current heart rate.
     * 
     * @return The calculated Beats Per Minute (BPM), or -1 if the reading failed.
     */
    public int getCurrentHeartRate() {
        System.out.println("Activating PPG sensor...");
        
        // 1. Read raw optical sensor data
        byte[] rawPpgData = readPpgSensor();
        
        if (rawPpgData == null || rawPpgData.length == 0) {
            System.err.println("Sensor Error: Could not read PPG data. Is the wearable on the wrist?");
            return -1;
        }
        
        System.out.println("Captured " + rawPpgData.length + " bytes of raw PPG data. Processing...");
        
        // 2. Pass the raw buffer to the native DSP algorithm to calculate BPM
        int currentBpm = calculateBpm(rawPpgData);
        
        if (currentBpm > 0) {
            System.out.println("Heart rate calculated: " + currentBpm + " BPM");
        } else {
            System.err.println("DSP Error: Too much motion artifact to calculate a stable BPM.");
        }
        
        return currentBpm;
    }
}