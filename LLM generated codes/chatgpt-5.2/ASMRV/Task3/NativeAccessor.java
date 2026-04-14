// File: NativeAccessor.java
public class NativeAccessor {

    static {
        System.loadLibrary("native_accessor");
    }

    // Native method declaration
    public native int getFieldValue(DataHolder holder);

    public static void main(String[] args) {
        DataHolder holder = new DataHolder(42);
        NativeAccessor accessor = new NativeAccessor();

        int result = accessor.getFieldValue(holder);
        System.out.println("Retrieved value from native code: " + result);
    }
}
