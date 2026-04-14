public class NativeTransformPipeline {

    static {
        System.loadLibrary("nativetransform");
    }

    // --- Native pipeline stages (already available capabilities) ---

    // 1. Read and interpret low-level data
    private native int[] nativeReadAndInterpret(byte[] rawData);

    // 2. Apply transformation rules
    private native int[] nativeApplyTransform(int[] interpretedData);

    // 3. Generate transformed output
    private native byte[] nativeGenerateOutput(int[] transformedData);

    // --- High-level pipeline exposed to Java ---

    public byte[] transform(byte[] rawData) {
        int[] interpreted = nativeReadAndInterpret(rawData);
        int[] transformed = nativeApplyTransform(interpreted);
        return nativeGenerateOutput(transformed);
    }

    // Example usage
    public static void main(String[] args) {
        NativeTransformPipeline pipeline = new NativeTransformPipeline();

        byte[] input = { 10, 20, 30, 40 };
        byte[] output = pipeline.transform(input);

        System.out.println("Transformed output:");
        for (byte b : output) {
            System.out.print(b + " ");
        }
    }
}
