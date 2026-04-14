/**
 * A simple product class with multiple fields.
 * The native method will locate and retrieve the 'quantity' integer field.
 */
public class Product {

    public String name;
    public int    quantity;      // <-- this is the field the native code will retrieve
    public double price;
    public String category;

    public Product(String name, int quantity, double price, String category) {
        this.name     = name;
        this.quantity = quantity;
        this.price    = price;
        this.category = category;
    }

    @Override
    public String toString() {
        return String.format(
            "Product{ name='%s', quantity=%d, price=%.2f, category='%s' }",
            name, quantity, price, category
        );
    }
}