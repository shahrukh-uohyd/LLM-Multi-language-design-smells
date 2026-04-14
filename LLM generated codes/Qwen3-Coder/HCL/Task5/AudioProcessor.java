public class AudioProcessor {
    
    static {
        loadAudioLibrary();
    }
    
    // Native method declarations for audio processing
    private static native boolean initializeDecoder();
    private static native int decodeAudio(String filePath, byte[] outputBuffer);
    private static native boolean setSampleRate(int sampleRate);
    private static native void cleanupDecoder();
    
    /**
     * Initializes the audio decoder
     */
    public static boolean init() {
        return initializeDecoder();
    }
    
    /**
     * Decodes audio file to raw PCM data
     */
    public static int decode(String filePath, byte[] outputBuffer) {
        return decodeAudio(filePath, outputBuffer);
    }
    
    /**
     * Sets desired sample rate for decoding
     */
    public static boolean setSampleRateHz(int sampleRate) {
        return setSampleRate(sampleRate);
    }
    
    /**
     * Cleans up decoder resources
     */
    public static void shutdown() {
        cleanupDecoder();
    }
    
    private static void loadAudioLibrary() {
        String osName = System.getProperty("os.name").toLowerCase();
        String libName;
        
        if (osName.contains("win")) {
            libName = "audio_processor_win";
        } else if (osName.contains("mac")) {
            libName = "libaudio_processor_mac";
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            libName = "libaudio_processor_linux";
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + osName);
        }
        
        try {
            System.loadLibrary(libName);
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException("Failed to load audio processor library: " + libName, e);
        }
    }
}