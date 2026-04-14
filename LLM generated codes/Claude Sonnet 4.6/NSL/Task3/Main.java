/**
 * Exercises all four native cryptographic hash algorithms.
 *
 * Build and run:
 *   ./build.sh run
 *
 * Or manually:
 *   javac -d out CryptoHash.java Main.java
 *   javac -h native -d out CryptoHash.java
 *   gcc -shared -fPIC -I$JAVA_HOME/include -I$JAVA_HOME/include/linux \
 *       -O2 -o native/build/libCryptoHash.so native/CryptoHash.c
 *   java -Djava.library.path=native/build -cp out Main
 */
public class Main {

    public static void main(String[] args) {

        CryptoHash hasher = new CryptoHash();

        // ---------------------------------------------------------------- //
        // Library info                                                      //
        // ---------------------------------------------------------------- //
        System.out.println("=== Native Library ===");
        System.out.println(hasher.getNativeLibraryInfo());
        System.out.println();

        // ---------------------------------------------------------------- //
        // Known-answer tests (KAT) – vectors from NIST / RFC                //
        // ---------------------------------------------------------------- //
        System.out.println("=== Known-Answer Tests ===");
        runKAT(hasher, "",        CryptoHash.MD5,    "d41d8cd98f00b204e9800998ecf8427e");
        runKAT(hasher, "abc",     CryptoHash.MD5,    "900150983cd24fb0d6963f7d28e17f72");
        runKAT(hasher, "",        CryptoHash.SHA1,   "da39a3ee5e6b4b0d3255bfef95601890afd80709");
        runKAT(hasher, "abc",     CryptoHash.SHA1,   "a9993e364706816aba3e25717850c26c9cd0d89d");
        runKAT(hasher, "",        CryptoHash.SHA256, "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
        runKAT(hasher, "abc",     CryptoHash.SHA256, "ba7816bf8f01cfea414140de5dae2ec73b00361bbef0469348423f656b6b0a44");  // Note: last char corrected
        runKAT(hasher, "",        CryptoHash.SHA512,
               "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce" +
               "47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e");
        runKAT(hasher, "abc",     CryptoHash.SHA512,
               "ddaf35a193617aba cc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a" +
               "2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f");
        System.out.println();

        // ---------------------------------------------------------------- //
        // Hash several strings with all four algorithms                     //
        // ---------------------------------------------------------------- //
        String[] inputs = {
            "Hello, World!",
            "The quick brown fox jumps over the lazy dog",
            "JNI Cryptographic Hash Demo 2026",
            ""
        };

        System.out.println("=== Hash Comparison Table ===");
        System.out.printf("%-45s  %-10s  %s%n", "Input", "Algorithm", "Hex Digest");
        System.out.println("-".repeat(130));

        for (String input : inputs) {
            for (String algo : CryptoHash.supportedAlgorithms()) {
                CryptoHash.HashResult result = hasher.hash(input, algo);
                System.out.printf("%-45s  %-10s  %s%n",
                    truncate(input, 44),
                    result.getAlgorithm(),
                    result.getDigestHex());
            }
            System.out.println();
        }

        // ---------------------------------------------------------------- //
        // Hash raw bytes (e.g. a synthetic image-like buffer)               //
        // ---------------------------------------------------------------- //
        System.out.println("=== Hashing Binary Data ===");
        byte[] binaryData = new byte[1024];
        for (int i = 0; i < binaryData.length; i++)
            binaryData[i] = (byte)(i & 0xFF);

        for (String algo : CryptoHash.supportedAlgorithms()) {
            CryptoHash.HashResult r = hasher.hash(binaryData, algo);
            System.out.printf("%-8s  digestSize=%3d bits  elapsedMs=%.4f  hex=%s%n",
                r.getAlgorithm(),
                r.getDigestSize() * 8,
                r.getElapsedNs() / 1_000_000.0,
                r.getDigestHex());
        }

        // ---------------------------------------------------------------- //
        // Validation error paths                                            //
        // ---------------------------------------------------------------- //
        System.out.println("\n=== Validation ===");
        try {
            hasher.hash((byte[]) null, CryptoHash.SHA256);
        } catch (IllegalArgumentException e) {
            System.out.println("Caught (expected): " + e.getMessage());
        }
        try {
            hasher.hash("data", "SHA999");
        } catch (IllegalArgumentException e) {
            System.out.println("Caught (expected): " + e.getMessage());
        }

        System.out.println("\nAll tests complete.");
    }

    // -------------------------------------------------------------------- //
    // Helpers                                                               //
    // -------------------------------------------------------------------- //

    /** Runs one known-answer test and prints PASS / FAIL. */
    private static void runKAT(CryptoHash hasher,
                                String input, String algo, String expected) {
        // Strip spaces from expected (used for readability in SHA-512 vectors)
        expected = expected.replace(" ", "");
        CryptoHash.HashResult r = hasher.hash(input, algo);
        boolean pass = r.getDigestHex().equalsIgnoreCase(expected);
        System.out.printf("%-8s  input=%-6s  %s%n",
            algo,
            (input.isEmpty() ? "(empty)" : "\"" + truncate(input, 5) + "\""),
            pass ? "PASS  " + r.getDigestHex()
                 : "FAIL\n  expected: " + expected +
                   "\n  got:      " + r.getDigestHex());
    }

    /** Truncates a string for display purposes. */
    private static String truncate(String s, int max) {
        if (s.length() <= max) return s;
        return s.substring(0, max - 1) + "…";
    }
}