public class NativeTrigger {

    static {
        System.loadLibrary("nativetrigger");
    }

    // Native entry point
    public native void triggerOperation();

    public static void main(String[] args) {
        NativeTrigger trigger = new NativeTrigger();
        trigger.triggerOperation();
    }
}
