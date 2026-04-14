// InventoryManager.java (Example usage)
import java.util.*;

public class InventoryManager {
    private List<Product> products;
    private Random random = new Random();
    
    public InventoryManager() {
        this.products = new ArrayList<>();
        initializeProducts();
    }
    
    private void initializeProducts() {
        // Create sample products with varying stock levels
        products.add(new Product("PROD001", "Laptop", 5, 999.99));
        products.add(new Product("PROD002", "Mouse", 15, 29.99));
        products.add(new Product("PROD003", "Keyboard", 3, 79.99));
        products.add(new Product("PROD004", "Monitor", 25, 299.99));
        products.add(new Product("PROD005", "Headphones", 8, 149.99));
        products.add(new Product("PROD006", "Webcam", 2, 89.99));
    }
    
    public void processLowStockItems() {
        System.out.println("Checking inventory for low stock items...\n");
        
        for (Product product : products) {
            System.out.println("Processing: " + product);
            int originalStock = product.getStockCount();
            
            // Call native method to handle low stock logic
            product.updateLowStock();
            
            int newStock = product.getStockCount();
            if (newStock != originalStock) {
                System.out.println("  Stock updated: " + originalStock + " -> " + newStock);
            } else {
                System.out.println("  Stock unchanged: " + originalStock);
            }
            System.out.println();
        }
    }
    
    public static void main(String[] args) {
        InventoryManager manager = new InventoryManager();
        manager.processLowStockItems();
    }
}