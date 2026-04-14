// ObjectInspector.java
public class ObjectInspector {

    static {
        System.loadLibrary("objectinspector");
    }

    // Native method receives an object instance and returns inspection info
    public native String inspectObject(Object obj);

    public static void main(String[] args) {
        UserAccount account = new UserAccount("jdoe", 28, true);

        ObjectInspector inspector = new ObjectInspector();
        String report = inspector.inspectObject(account);

        // Use inspection result in Java
        System.out.println("Inspection Report:");
        System.out.println(report);
    }
}
