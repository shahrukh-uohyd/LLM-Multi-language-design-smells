public class AudioDecoder implements AutoCloseable {

    // 1. Static block to load the correct OS-specific native library
    static {
        String osName = System.getProperty("os.name").toLowerCase();
        String libName;

        // Determine the correct library name based on the OS
        if (osName.contains("win")) {
            // Windows: JVM will look for "audio_codec_win.dll"
            libName = "audio_codec_win";
        } else if (osName.contains("mac")) {
            // macOS: JVM will look for "libaudio_codec_mac.dylib"
            libName = "audio_codec_mac";
        } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
            // Linux/Unix: JVM will look for "libaudio_codec_linux.so"
            libName = "audio_codec_linux";
        } else {
            throw new UnsupportedOperationException("Unsupported Operating System for Audio Decoder: " + osName);
        }

        try {
            System.loadLibrary(libName);
            System.out.println("Multimedia Audio Decoder initialized successfully using: " + libName);
        } catch (UnsatisfiedLinkError e) {
            System.err.println("CRITICAL ERROR: Failed to load native audio library: " + libName);
            System.err.println("Please ensure the appropriate .dll, .so, or .dylib file is in the java.library.path");
            throw new RuntimeException("Audio Decoder native library missing.", e);
        }
    }

    private boolean isInitialized = false;

    // 2. Native method declarations (implemented in C/C++/Rust)
    private native boolean nativeInitDecoder(int sampleRate, int channels);
    private native byte[] nativeDecodeFrame(byte[] compressedData);
    private native void nativeReleaseDecoder();

    // 3. Java wrappers for the audio processing features
    
    /**
     * Initializes the native audio engine with target specifications.
     * 
     * @param sampleRate The target sample rate (e.g., 44100 or 48000)
     * @param channels   The number of audio channels (1 for Mono, 2 for Stereo)
     */
    public void initialize(int sampleRate, int channels) {
        if (isInitialized) {
            throw new IllegalStateException("Audio decoder is already initialized.");
        }
        
        boolean success = nativeInitDecoder(sampleRate, channels);
        if (!success) {
            throw new RuntimeException("Native audio decoder failed to initialize.");
        }
        isInitialized = true;
    }

    /**
     * Decodes a compressed audio frame (e.g., MP3, AAC) into raw PCM bytes.
     * 
     * @param compressedData The compressed audio byte payload.
     * @return Decoded raw PCM data.
     */
    public byte[] decode(byte[] compressedData) {
        if (!isInitialized) {
            throw new IllegalStateException("Cannot decode before initializing the audio decoder.");
        }
        if (compressedData == null || compressedData.length == 0) {
            throw new IllegalArgumentException("Compressed audio data cannot be null or empty.");
        }
        
        return nativeDecodeFrame(compressedData);
    }

    /**
     * Safely releases native memory/resources used by the decoder.
     * Implements AutoCloseable for use in try-with-resources blocks.
     */
    @Override
    public void close() {
        if (isInitialized) {
            nativeReleaseDecoder();
            isInitialized = false;
            System.out.println("Audio Decoder resources released.");
        }
    }
}