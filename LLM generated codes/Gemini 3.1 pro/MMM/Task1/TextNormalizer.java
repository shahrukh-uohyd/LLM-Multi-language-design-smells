public class TextNormalizer {

    // Load the native shared library
    static {
        System.loadLibrary("textnormalizer");
    }

    /**
     * Native method that takes a single String and returns its normalized form.
     * 
     * @param input The raw textual value.
     * @return The normalized textual value.
     */
    public native String normalize(String input);

    public static void main(String[] args) {
        TextNormalizer normalizer = new TextNormalizer();

        // Prepare a set of textual values
        String[] rawInputs = {
            "   Hello World   ",
            "JNI is AWESOME!",
            "\t  Trailing Space Example  \n",
            "   mixed CASE input   ",
            "NoSpacesToTrim"
        };

        System.out.println("--- JNI Text Normalization ---");
        
        // Pass one textual value at a time to the native method
        for (String text : rawInputs) {
            String normalizedText = normalizer.normalize(text);
            System.out.println("Original  : '" + text + "'");
            System.out.println("Normalized: '" + normalizedText + "'\n");
        }
    }
}