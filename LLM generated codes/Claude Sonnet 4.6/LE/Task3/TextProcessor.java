/**
 * Exposes a native C text-processing pipeline to Java.
 *
 * <h2>Operation</h2>
 * Given a text-based input string, the pipeline:
 * <ol>
 *   <li><b>Parse</b>   – splits the input into structured elements
 *       ({@link #nativeParse}).</li>
 *   <li><b>Process</b> – applies predefined logic to those elements
 *       ({@link #nativeProcess}).</li>
 *   <li><b>Generate</b> – derives the final output value from the
 *       processed elements ({@link #nativeGenerate}).</li>
 * </ol>
 *
 * <h2>Inter-stage wire format</h2>
 * The intermediate representation (IR) passed between stages is an
 * opaque {@code byte[]} whose internal layout is a private contract
 * between the three C functions.  Java code never inspects it.
 *
 * <h2>Thread safety</h2>
 * Each {@link #execute} call is stateless on the Java side.  The C
 * implementation is likewise stateless; thread safety of any shared C
 * globals is the responsibility of the native layer.
 *
 * <h2>Quick start</h2>
 * <pre>
 *   TextProcessor tp = new TextProcessor();
 *   String result = tp.execute("word1 word2 word3");
 *   System.out.println(result);
 * </pre>
 */
public class TextProcessor {

    // ------------------------------------------------------------------ //
    //  Library bootstrap                                                   //
    // ------------------------------------------------------------------ //
    static {
        /*
         * Loads:
         *   libTextProcessor.so    on Linux
         *   libTextProcessor.dylib on macOS
         *   TextProcessor.dll      on Windows
         *
         * Ensure -Djava.library.path=<dir> points to the directory
         * containing the shared library, or place it on LD_LIBRARY_PATH.
         */
        System.loadLibrary("TextProcessor");
    }

    // ------------------------------------------------------------------ //
    //  Native method declarations                                          //
    // ------------------------------------------------------------------ //

    /**
     * Stage 1 — Parse the input text into structured elements.
     *
     * <p>The C function tokenises {@code inputText} and serialises the
     * resulting elements into an opaque intermediate representation (IR).
     * The IR is consumed exclusively by {@link #nativeProcess}.
     *
     * @param inputText the raw text to parse; must not be {@code null}
     * @return opaque IR byte array representing the structured elements
     * @throws ProcessingException (stage=PARSE) if the text is malformed
     *         or empty
     */
    private native byte[] nativeParse(String inputText);

    /**
     * Stage 2 — Apply predefined logic to the structured elements.
     *
     * <p>The C function consumes the IR from {@link #nativeParse}, applies
     * all processing rules, and returns a new IR representing the processed
     * state.
     *
     * @param parsedData opaque byte array returned by {@link #nativeParse}
     * @return opaque IR byte array representing the processed elements
     * @throws ProcessingException (stage=PROCESS) if processing rules
     *         cannot be applied
     */
    private native byte[] nativeProcess(byte[] parsedData);

    /**
     * Stage 3 — Generate the final output from the processed elements.
     *
     * <p>The C function derives a single output {@code String} from the IR
     * produced by {@link #nativeProcess}.
     *
     * @param processedData opaque byte array returned by {@link #nativeProcess}
     * @return the final derived output value; never {@code null}
     * @throws ProcessingException (stage=GENERATE) if the output cannot be
     *         produced
     */
    private native String nativeGenerate(byte[] processedData);

    // ------------------------------------------------------------------ //
    //  Public high-level operation                                         //
    // ------------------------------------------------------------------ //

    /**
     * Processes {@code inputText} through the full three-stage native pipeline
     * and returns the derived output value.
     *
     * <pre>
     *   inputText ──parse──► IR ──process──► IR' ──generate──► result
     * </pre>
     *
     * @param inputText text-based input; must not be {@code null} or empty
     * @return the derived output value string
     * @throws IllegalArgumentException if {@code inputText} is {@code null}
     *         or blank
     * @throws ProcessingException      if any native stage fails
     */
    public String execute(String inputText) {
        if (inputText == null || inputText.isBlank()) {
            throw new IllegalArgumentException("inputText must not be null or blank");
        }

        // Stage 1: parse input text into structured elements
        byte[] parsedData = nativeParse(inputText);

        // Stage 2: process elements according to predefined logic
        byte[] processedData = nativeProcess(parsedData);

        // Stage 3: generate and return the final output value
        return nativeGenerate(processedData);
    }

    // ------------------------------------------------------------------ //
    //  Smoke-test entry point                                              //
    // ------------------------------------------------------------------ //

    /**
     * Minimal smoke test — run with:
     * <pre>java -Djava.library.path=. TextProcessor</pre>
     */
    public static void main(String[] args) {
        TextProcessor processor = new TextProcessor();

        String[] testInputs = {
            "hello world foo bar",
            "alpha beta gamma delta epsilon",
            "one two three"
        };

        for (String input : testInputs) {
            String result = processor.execute(input);
            System.out.printf("Input  : \"%s\"%n", input);
            System.out.printf("Output : \"%s\"%n%n", result);
        }
    }
}