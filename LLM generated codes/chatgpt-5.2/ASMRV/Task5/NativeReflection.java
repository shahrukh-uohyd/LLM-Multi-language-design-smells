// File: NativeReflection.java
public class NativeReflection {

    static {
        System.loadLibrary("native_reflection");
    }

    // Native method that retrieves information about a class member
    public native String getFieldInfo(String className, String fieldName);

    public static void main(String[] args) {
        NativeReflection nr = new NativeReflection();

        String info1 = nr.getFieldInfo("Person", "age");
        String info2 = nr.getFieldInfo("Person", "name");

        System.out.println(info1);
        System.out.println(info2);
    }
}
