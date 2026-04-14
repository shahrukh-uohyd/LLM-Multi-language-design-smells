public class Sample {

    private int counter;

    public Sample(int counter) {
        this.counter = counter;
    }

    public void updateCounter(int value) {
        System.out.println("Updated counter: " + value);
    }

    // Native method
    public native void nativeInspectAndInvoke();
}
