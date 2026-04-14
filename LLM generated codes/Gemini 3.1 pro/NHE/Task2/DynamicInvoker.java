public class DynamicInvoker {
    
    // Load the native shared library
    static {
        System.loadLibrary("dynamicinvoker"); 
    }

    // Native method that determines which method to invoke based on 'methodName'
    // It takes the target object, the method name, and two integer arguments.
    public native int invokeMethodDynamically(MathOperations obj, String methodName, int a, int b);

    public static void main(String[] args) {
        DynamicInvoker invoker = new DynamicInvoker();
        MathOperations mathOps = new MathOperations();
        
        System.out.println("[Java] Requesting native code to invoke 'add'...");
        int sum = invoker.invokeMethodDynamically(mathOps, "add", 15, 5);
        System.out.println("[Java] Result returned to Java: " + sum + "\n");
        
        System.out.println("[Java] Requesting native code to invoke 'multiply'...");
        int product = invoker.invokeMethodDynamically(mathOps, "multiply", 15, 5);
        System.out.println("[Java] Result returned to Java: " + product + "\n");

        System.out.println("[Java] Requesting native code to invoke 'subtract'...");
        int difference = invoker.invokeMethodDynamically(mathOps, "subtract", 15, 5);
        System.out.println("[Java] Result returned to Java: " + difference);
    }
}