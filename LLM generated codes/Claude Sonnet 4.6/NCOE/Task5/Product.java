public class Product {

    public String productId;
    public String name;
    public int    stockCount;
    public double unitCost;

    public Product(String productId, String name, int stockCount, double unitCost) {
        this.productId  = productId;
        this.name       = name;
        this.stockCount = stockCount;
        this.unitCost   = unitCost;
    }

    @Override
    public String toString() {
        return String.format(
            "Product{id='%s', name='%s', stockCount=%d, unitCost=%.2f}",
            productId, name, stockCount, unitCost);
    }
}