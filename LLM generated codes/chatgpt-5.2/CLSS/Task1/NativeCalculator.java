public class NativeCalculator {

    static {
        System.loadLibrary("nativecalc");
    }

    // Native entry point
    public native void startCalculation(StatusHandler handler);

    public static void main(String[] args) {

        NativeCalculator calc = new NativeCalculator();

        calc.startCalculation((code, message) -> {
            System.out.println("Status [" + code + "]: " + message);
        });
    }
}
