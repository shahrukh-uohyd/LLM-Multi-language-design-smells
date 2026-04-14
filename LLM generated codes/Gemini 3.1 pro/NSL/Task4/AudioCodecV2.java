public class AudioCodecV2 {

    // The static block executes when the class is first loaded by the JVM.
    // This ensures the native library is ready before any native methods are called.
    static {
        // Loads libaudio-codec-v2.so (Linux), libaudio-codec-v2.dylib (macOS), 
        // or audio-codec-v2.dll (Windows) from the java.library.path
        System.loadLibrary("audio-codec-v2");
    }

    /**
     * Initializes the proprietary native audio decoder.
     * 
     * @return A native pointer/handle (represented as a long) to the decoder instance.
     */
    public native long initializeDecoder();

    /**
     * Decodes an entire proprietary audio file into raw PCM audio bytes.
     * 
     * @param filePath The absolute or relative path to the proprietary audio file.
     * @return A byte array containing the decoded raw PCM audio data.
     */
    public native byte[] decodeFile(String filePath);

    /**
     * Decodes a chunk of proprietary audio data (useful for streaming).
     * 
     * @param decoderHandle The native pointer/handle returned by initializeDecoder().
     * @param encodedData   The raw, encoded byte chunk read from the stream.
     * @return A byte array containing the decoded raw PCM audio data for this chunk.
     */
    public native byte[] decodeChunk(long decoderHandle, byte[] encodedData);

    /**
     * Cleans up and releases the memory/resources held by the native decoder.
     * 
     * @param decoderHandle The native pointer/handle to the decoder instance.
     */
    public native void releaseDecoder(long decoderHandle);

    public static void main(String[] args) {
        // Accessing the class for the first time will trigger the static block 
        // and attempt to load the 'audio-codec-v2' library.
        AudioCodecV2 codec = new AudioCodecV2();
        
        System.out.println("AudioCodecV2 class initialized and native library loaded.");
        
        // Example workflow (commented out as it requires the actual native implementation to run):
        /*
        long handle = codec.initializeDecoder();
        try {
            byte[] pcmData = codec.decodeFile("track01.proprietary");
            System.out.println("Decoded " + pcmData.length + " bytes of PCM audio data.");
        } finally {
            codec.releaseDecoder(handle);
        }
        */
    }
}