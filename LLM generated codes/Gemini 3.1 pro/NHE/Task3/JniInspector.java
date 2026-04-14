public class JniInspector {

    // Load the native shared library
    static {
        System.loadLibrary("jniinspector");
    }

    // Native method that receives an arbitrary Object, inspects its structure,
    // and returns a formatted string containing the inspection results.
    public native String inspectObject(Object obj);

    public static void main(String[] args) {
        JniInspector inspector = new JniInspector();
        
        // Create an instance of the target class
        TargetObject target = new TargetObject(101, "Alice", 2500.50, true);
        
        System.out.println("[Java] Passing object to native code for inspection...\n");
        
        // The native method inspects the object and returns the structural data
        String inspectionReport = inspector.inspectObject(target);
        
        // The result is used/printed in Java
        System.out.println("[Java] Received Inspection Report from Native Code:\n");
        System.out.println(inspectionReport);
    }
}