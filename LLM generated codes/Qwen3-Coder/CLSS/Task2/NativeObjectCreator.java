public class NativeObjectCreator {
    static {
        System.loadLibrary("objectcreator"); // Load your native library
    }
    
    // JNI method that returns a new DataContainer instance
    public native DataContainer createDataContainer(float value);
}