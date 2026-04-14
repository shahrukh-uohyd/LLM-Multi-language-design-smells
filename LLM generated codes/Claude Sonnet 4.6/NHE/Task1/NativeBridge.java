public class NativeBridge {

    static {
        System.loadLibrary("nativebridge");
    }

    public native String getPersonName(Person person);
}