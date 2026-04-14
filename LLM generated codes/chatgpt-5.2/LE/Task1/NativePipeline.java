public class NativePipeline {

    static {
        System.loadLibrary("nativepipeline");
    }

    // ----- Native capabilities already available -----

    // Parse raw input into internal representation
    private native double[] nativeParse(String rawInput);

    // Normalize parsed data
    private native double[] nativeNormalize(double[] parsedData);

    // Compute final result
    private native double nativeCompute(double[] normalizedData);

    // ----- High-level operation required by the system -----

    public double process(String rawInput) {
        double[] parsed = nativeParse(rawInput);
        double[] normalized = nativeNormalize(parsed);
        return nativeCompute(normalized);
    }

    // Demo
    public static void main(String[] args) {
        NativePipeline pipeline = new NativePipeline();
        double result = pipeline.process("10,20,30,40");
        System.out.println("Final result: " + result);
    }
}
