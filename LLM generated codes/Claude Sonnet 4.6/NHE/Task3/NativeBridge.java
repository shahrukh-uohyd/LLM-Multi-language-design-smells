public class NativeBridge {

    static {
        System.loadLibrary("nativebridge");
    }

    /**
     * Native method: receives an Employee instance and inspects it.
     * Reads every field directly via JNI field access,
     * then builds and returns a formatted inspection report string.
     *
     * @param obj the Employee object to inspect
     * @return    a multi-line inspection report built in native code
     */
    public native String inspect(Employee obj);
}