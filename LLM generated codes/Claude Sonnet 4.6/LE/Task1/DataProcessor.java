/**
 * High-level data processing facade.
 *
 * Native pipeline (C++ via JNI):
 *   1. parse()      – converts raw input into an internal representation
 *   2. normalize()  – applies predefined normalization rules
 *   3. compute()    – derives the final result
 */
public class DataProcessor {

    // ------------------------------------------------------------------ //
    //  Load the native shared library once per class-loader               //
    // ------------------------------------------------------------------ //
    static {
        System.loadLibrary("dataprocessor"); // libdataprocessor.so / dataprocessor.dll
    }

    // ------------------------------------------------------------------ //
    //  Native method declarations (implemented in DataProcessor.cpp)      //
    // ------------------------------------------------------------------ //

    /**
     * Parses {@code rawInput} and returns an opaque internal representation
     * encoded as a byte array (e.g. serialised struct / JSON / protobuf).
     *
     * @param rawInput raw string data to be parsed
     * @return internal representation as bytes; never {@code null}
     * @throws IllegalArgumentException if {@code rawInput} is malformed
     */
    private native byte[] parse(String rawInput);

    /**
     * Normalizes the internal representation produced by {@link #parse}.
     *
     * @param parsed byte array returned by {@link #parse}
     * @return normalized byte array; never {@code null}
     */
    private native byte[] normalize(byte[] parsed);

    /**
     * Computes the final result from the normalized data.
     *
     * @param normalized byte array returned by {@link #normalize}
     * @return computed result as a {@code String}
     */
    private native String compute(byte[] normalized);

    // ------------------------------------------------------------------ //
    //  Public high-level operation                                        //
    // ------------------------------------------------------------------ //

    /**
     * Processes {@code rawInput} through the full native pipeline and
     * returns the final computed result.
     *
     * <pre>
     *   rawInput ──parse──▶ internal ──normalize──▶ normalized ──compute──▶ result
     * </pre>
     *
     * @param rawInput structured input data (must not be {@code null})
     * @return final computed result
     * @throws IllegalArgumentException if {@code rawInput} is {@code null} or malformed
     */
    public String process(String rawInput) {
        if (rawInput == null) {
            throw new IllegalArgumentException("rawInput must not be null");
        }

        byte[] parsed     = parse(rawInput);
        byte[] normalized = normalize(parsed);
        return compute(normalized);
    }

    // ------------------------------------------------------------------ //
    //  Quick smoke-test entry point                                       //
    // ------------------------------------------------------------------ //
    public static void main(String[] args) {
        DataProcessor processor = new DataProcessor();

        String input  = "key1=10;key2=20;key3=30";
        String result = processor.process(input);

        System.out.println("Input  : " + input);
        System.out.println("Result : " + result);
    }
}