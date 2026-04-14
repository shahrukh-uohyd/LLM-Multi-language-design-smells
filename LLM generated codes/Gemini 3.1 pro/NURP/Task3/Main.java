public class Main {
    public static void main(String[] args) {
        // 1. Load the native JNI library first
        AdvancedMath.loadLibrary();

        // 2. Instantiate the math class
        AdvancedMath math = new AdvancedMath();

        System.out.println("\n--- JNI Math Computations ---");
        
        // Power calculation
        double base = 2.5;
        double exp = 3.0;
        System.out.printf("%.2f ^ %.2f = %.4f\n", base, exp, math.power(base, exp));

        // Factorial calculation
        int factN = 15;
        System.out.printf("Factorial of %d = %d\n", factN, math.factorial(factN));

        // Fibonacci calculation
        int fibN = 50;
        System.out.printf("Fibonacci number %d = %d\n", fibN, math.fibonacci(fibN));
    }
}