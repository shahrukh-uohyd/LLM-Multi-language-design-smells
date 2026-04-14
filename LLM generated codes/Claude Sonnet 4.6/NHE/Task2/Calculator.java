public class Calculator {

    public int add(int a, int b) {
        return a + b;
    }

    public int subtract(int a, int b) {
        return a - b;
    }

    public int multiply(int a, int b) {
        return a * b;
    }

    public int divide(int a, int b) {
        if (b == 0) {
            System.out.println("[java] Division by zero is not allowed.");
            return 0;
        }
        return a / b;
    }

    @Override
    public String toString() {
        return "Calculator{add, subtract, multiply, divide}";
    }
}