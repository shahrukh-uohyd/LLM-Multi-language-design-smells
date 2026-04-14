// NativeAccessor.java
public class NativeAccessor {

    static {
        System.loadLibrary("nativeaccessor"); // loads libnativeaccessor.so / nativeaccessor.dll
    }

    // Native method receives a Person instance
    public native int getAgeFromNative(Person person);

    public static void main(String[] args) {
        Person person = new Person("Alice", 30, 75000.0);

        NativeAccessor accessor = new NativeAccessor();
        int ageFromNative = accessor.getAgeFromNative(person);

        // Use the value returned from native code
        System.out.println("Age retrieved via JNI: " + ageFromNative);
        System.out.println("Age * 2 (used in Java): " + (ageFromNative * 2));
    }
}
