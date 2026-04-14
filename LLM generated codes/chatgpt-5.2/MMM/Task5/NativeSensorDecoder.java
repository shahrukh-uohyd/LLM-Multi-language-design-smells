public class NativeSensorDecoder {

    static {
        System.loadLibrary("sensordecoder");
    }

    /**
     * Decodes a binary sensor data stream.
     *
     * @param stream raw sensor byte stream (4 bytes per reading)
     * @return decoded sensor readings
     */
    public static native int[] decode(byte[] stream);
}
