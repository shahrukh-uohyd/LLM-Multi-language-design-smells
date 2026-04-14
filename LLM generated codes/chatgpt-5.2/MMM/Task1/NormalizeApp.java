public class NormalizeApp {

    public static void main(String[] args) {

        String[] inputs = {
            "  hello world  ",
            "\tJava JNI\t",
            "native Code",
            "  mixed Case Text "
        };

        for (String input : inputs) {
            String normalized =
                NativeTextNormalizer.normalize(input);

            System.out.println(
                "Original: [" + input + "] -> Normalized: [" + normalized + "]");
        }
    }
}
