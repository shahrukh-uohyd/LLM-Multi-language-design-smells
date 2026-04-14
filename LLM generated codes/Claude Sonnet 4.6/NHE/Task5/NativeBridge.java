public class NativeBridge {

    static {
        System.loadLibrary("nativebridge");
    }

    /**
     * Native method: receives an array of Product objects.
     * For each element, invokes getTotalValue() from C++.
     * Returns a double[] of total values, one per Product.
     *
     * @param products array of Product objects to iterate
     * @return         array of total values retrieved natively
     */
    public native double[] processProducts(Product[] products);
}