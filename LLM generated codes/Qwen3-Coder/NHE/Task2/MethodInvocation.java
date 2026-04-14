/**
 * Java class containing several methods to demonstrate dynamic invocation through JNI
 */
public class MethodInvocation {
    private String name;
    private int value;

    public MethodInvocation(String name, int value) {
        this.name = name;
        this.value = value;
    }

    // Instance methods
    public String greet() {
        return "Hello, my name is " + name;
    }

    public int calculateSquare() {
        return value * value;
    }

    public String formatInfo() {
        return "Name: " + name + ", Value: " + value;
    }

    public boolean isPositive() {
        return value > 0;
    }

    // Static methods
    public static String getStaticMessage() {
        return "This is a static method";
    }

    public static int add(int a, int b) {
        return a + b;
    }

    // Native method for dynamic invocation
    public native Object invokeMethod(Object obj, String methodName, Class<?>[] paramTypes, Object... args);
    
    // Native method for static method invocation
    public native Object invokeStaticMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes, Object... args);

    // Load the native library
    static {
        System.loadLibrary("methodinvocation");
    }

    @Override
    public String toString() {
        return "MethodInvocation{" +
                "name='" + name + '\'' +
                ", value=" + value +
                '}';
    }

    public static void main(String[] args) {
        // Create an instance of MethodInvocation
        MethodInvocation obj = new MethodInvocation("John", 5);
        MethodInvocation invoker = new MethodInvocation("", 0);

        System.out.println("Testing dynamic method invocation:");
        System.out.println("Object: " + obj);
        
        // Test instance method invocations
        try {
            String greeting = (String) invoker.invokeMethod(obj, "greet", new Class[0]);
            System.out.println("Result of greet(): " + greeting);

            Integer square = (Integer) invoker.invokeMethod(obj, "calculateSquare", new Class[0]);
            System.out.println("Result of calculateSquare(): " + square);

            String info = (String) invoker.invokeMethod(obj, "formatInfo", new Class[0]);
            System.out.println("Result of formatInfo(): " + info);

            Boolean positive = (Boolean) invoker.invokeMethod(obj, "isPositive", new Class[0]);
            System.out.println("Result of isPositive(): " + positive);
            
            // Test with parameters - changing the value
            MethodInvocation obj2 = new MethodInvocation("Jane", -3);
            Boolean positive2 = (Boolean) invoker.invokeMethod(obj2, "isPositive", new Class[0]);
            System.out.println("Result of isPositive() on negative value: " + positive2);
        } catch (Exception e) {
            System.err.println("Error invoking instance method: " + e.getMessage());
        }

        System.out.println("\nTesting static method invocation:");
        
        // Test static method invocations
        try {
            String staticMsg = (String) invoker.invokeStaticMethod(
                MethodInvocation.class, 
                "getStaticMessage", 
                new Class[0]
            );
            System.out.println("Result of getStaticMessage(): " + staticMsg);

            Integer sum = (Integer) invoker.invokeStaticMethod(
                MethodInvocation.class, 
                "add", 
                new Class[]{int.class, int.class}, 
                10, 20
            );
            System.out.println("Result of add(10, 20): " + sum);
        } catch (Exception e) {
            System.err.println("Error invoking static method: " + e.getMessage());
        }
    }
}