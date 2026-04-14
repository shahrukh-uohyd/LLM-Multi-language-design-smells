// Product.java
public class Product {
    private String productId;
    private String productName;
    private int stockCount;
    private double price;
    
    public Product(String productId, String productName, int initialStock, double price) {
        this.productId = productId;
        this.productName = productName;
        this.stockCount = initialStock;
        this.price = price;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public String getProductName() {
        return productName;
    }
    
    public int getStockCount() {
        return stockCount;
    }
    
    public double getPrice() {
        return price;
    }
    
    // Native method declaration
    public native void updateLowStock();
    
    static {
        System.loadLibrary("inventory_system"); // Load native library
    }
    
    @Override
    public String toString() {
        return "Product{id='" + productId + "', name='" + productName + "', stock=" + stockCount + ", price=" + price + "}";
    }
}