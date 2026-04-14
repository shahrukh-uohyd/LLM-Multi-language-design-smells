public class Product {

    String  name;
    double  price;
    int     quantity;
    boolean available;

    public Product(String name, double price, int quantity, boolean available) {
        this.name      = name;
        this.price     = price;
        this.quantity  = quantity;
        this.available = available;
    }

    /**
     * The method native code will invoke on each Product element.
     * Computes total value = price * quantity.
     *
     * @return total stock value of this product
     */
    public double getTotalValue() {
        return price * quantity;
    }

    public String getName()   { return name;      }
    public double getPrice()  { return price;     }
    public int getQuantity()  { return quantity;  }
    public boolean isAvailable() { return available; }

    @Override
    public String toString() {
        return String.format(
            "Product{name='%s', price=%.2f, qty=%d, available=%b}",
            name, price, quantity, available);
    }
}