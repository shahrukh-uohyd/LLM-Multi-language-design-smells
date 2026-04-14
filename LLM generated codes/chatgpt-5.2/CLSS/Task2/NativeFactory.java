public class NativeFactory {

    static {
        System.loadLibrary("nativefactory");
    }

    // Native method that creates a DataContainer instance
    public native DataContainer createDataContainer(double value);

    public static void main(String[] args) {
        NativeFactory factory = new NativeFactory();

        DataContainer container = factory.createDataContainer(42.75);

        System.out.println("Returned object: " + container);
        System.out.println("Value inside object: " + container.getValue());
    }
}
