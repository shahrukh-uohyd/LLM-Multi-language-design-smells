// MathComputation.java
/**
 * Java class that declares and interacts with native mathematical functions via JNI.
 * It handles the loading of the native library from a local directory.
 */
public class MathComputation {

    // Load the native library upon class loading.
    // The library "advanced_math" must be in the specified subdirectory.
    static {
        loadNativeLibrary();
    }

    /**
     * Locates and loads the native library from a subdirectory named "native".
     * This ensures the library is bundled with the application for portability.
     */
    private static void loadNativeLibrary() {
        try {
            String libPath = System.getProperty("user.dir") + 
                           System.getProperty("file.separator") + 
                           "native" +
                           System.getProperty("file.separator") + 
                           System.mapLibraryName("advanced_math");
            System.load(libPath);
        } catch (UnsatisfiedLinkError e) {
            throw new RuntimeException(
                "Failed to load native library 'advanced_math' from the 'native' subdirectory. " +
                "Ensure the library file (e.g., advanced_math.dll, libadvanced_math.so) exists there. " +
                "Error: " + e.getMessage(), e);
        }
    }

    /**
     * Calculates the factorial of a given number using native code.
     *
     * @param n The integer for which to calculate the factorial.
     * @return The factorial of n.
     */
    public static native long calculateFactorial(int n);

    /**
     * Computes a complex expression: (a^b) mod c using native code.
     *
     * @param base     The base value (a).
     * @param exponent The exponent value (b).
     * @param modulus  The modulus value (c).
     * @return The result of (a^b) mod c.
     */
    public static native long computeModularExponentiation(long base, long exponent, long modulus);

    /**
     * Generates a sequence of prime numbers up to a specified limit using native code.
     *
     * @param limit The upper bound for prime generation.
     * @return A string representation of the generated prime numbers.
     */
    public static native String generatePrimesUpTo(int limit);

    /**
     * The main method demonstrates how to use the native methods.
     */
    public static void main(String[] args) {
        // Example 1: Calculate Factorial
        int num = 5;
        long factorialResult = calculateFactorial(num);
        System.out.println("Factorial of " + num + " is: " + factorialResult);

        // Example 2: Compute Modular Exponentiation
        long base = 3;
        long exp = 4;
        long mod = 7;
        long modExpResult = computeModularExponentiation(base, exp, mod);
        System.out.println("(" + base + "^" + exp + ") % " + mod + " = " + modExpResult);

        // Example 3: Generate Primes
        int primeLimit = 20;
        String primes = generatePrimesUpTo(primeLimit);
        System.out.println("Prime numbers up to " + primeLimit + ": " + primes);
    }
}