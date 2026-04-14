public class TextProcessingPipeline {

    static {
        System.loadLibrary("textpipeline");
    }

    // --- Native steps (already implemented in C) ---

    // 1. Parse input text into structured elements
    private native int[] nativeParse(String input);

    // 2. Process structured elements
    private native int[] nativeProcess(int[] elements);

    // 3. Generate final output
    private native int nativeGenerate(int[] processed);

    // --- High-level operation exposed to Java ---

    public int derive(String input) {
        int[] parsed = nativeParse(input);
        int[] processed = nativeProcess(parsed);
        return nativeGenerate(processed);
    }

    // Example invocation
    public static void main(String[] args) {
        TextProcessingPipeline pipeline = new TextProcessingPipeline();
        int result = pipeline.derive("3,5,7,9");
        System.out.println("Derived output: " + result);
    }
}
