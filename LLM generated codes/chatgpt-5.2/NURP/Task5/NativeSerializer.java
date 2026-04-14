public class NativeSerializer {

    static {
        NativeLoader.load();
    }

    /**
     * Serializes a UTF-8 string into a byte array.
     */
    public static native byte[] serialize(String data);

    /**
     * Deserializes a byte array back into a UTF-8 string.
     */
    public static native String deserialize(byte[] bytes);
}
