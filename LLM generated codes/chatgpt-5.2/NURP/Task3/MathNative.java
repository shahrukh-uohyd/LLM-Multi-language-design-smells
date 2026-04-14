public class MathNative {

    static {
        NativeLoader.load();
    }

    // Advanced math operations implemented in native code
    public static native double fastPower(double base, double exponent);
    public static native double vectorMagnitude(double x, double y, double z);
}
