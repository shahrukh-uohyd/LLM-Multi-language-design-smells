public class Main {

    public static void main(String[] args) {

        Calculator calc   = new Calculator();
        NativeBridge bridge = new NativeBridge();

        System.out.println("=== JNI Dynamic Method Invocation Demo ===");
        System.out.println("Calculator object: " + calc);
        System.out.println();

        // Operations to demonstrate dynamic invocation
        String[] operations = { "add", "subtract", "multiply", "divide" };
        int a = 20, b = 4;

        for (String op : operations) {
            int result = bridge.invokeOperation(calc, op, a, b);
            System.out.println("[java] " + op + "(" + a + ", " + b + ") = " + result);
        }

        System.out.println();

        // Edge case: unknown operation
        System.out.println("--- Edge case: unknown operation ---");
        int result = bridge.invokeOperation(calc, "modulo", a, b);
        System.out.println("[java] modulo(" + a + ", " + b + ") = " + result);

        System.out.println();

        // Edge case: division by zero
        System.out.println("--- Edge case: divide by zero ---");
        result = bridge.invokeOperation(calc, "divide", a, 0);
        System.out.println("[java] divide(" + a + ", 0) = " + result);
    }
}