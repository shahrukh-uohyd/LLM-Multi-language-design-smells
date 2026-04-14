/**
 * Declares the native method that accepts a Product object,
 * locates the 'quantity' integer field inside it, and returns
 * the retrieved integer value back to Java.
 */
public class FieldAccessJNI {

    static {
        System.loadLibrary("FieldAccessJNI");
    }

    /**
     * Accepts a Product object, locates the 'quantity' field using
     * JNI field-access APIs, and returns its integer value.
     *
     * @param product  a Product instance whose 'quantity' field will be read
     * @return         the integer value of Product.quantity
     */
    public native int getQuantity(Product product);
}