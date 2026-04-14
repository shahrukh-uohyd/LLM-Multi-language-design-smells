// NativeDispatcher.java
public class NativeDispatcher {

    static {
        System.loadLibrary("nativedispatcher");
    }

    // Native method dynamically decides which Calculator method to call
    public native int invokeOperation(
            Calculator calculator,
            String operation,
            int a,
            int b
    );

    public static void main(String[] args) {
        Calculator calculator = new Calculator();
        NativeDispatcher dispatcher = new NativeDispatcher();

        int result1 = dispatcher.invokeOperation(calculator, "add", 4, 5);
        int result2 = dispatcher.invokeOperation(calculator, "multiply", 3, 7);
        int result3 = dispatcher.invokeOperation(calculator, "subtract", 10, 6);

        System.out.println("add result: " + result1);
        System.out.println("multiply result: " + result2);
        System.out.println("subtract result: " + result3);
    }
}
