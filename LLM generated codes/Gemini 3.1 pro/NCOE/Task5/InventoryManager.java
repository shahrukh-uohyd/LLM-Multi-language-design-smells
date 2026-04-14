package com.example.inventory;

public class InventoryManager {

    static {
        // Load the compiled C library (e.g., libinventory.so, inventory.dll, libinventory.dylib)
        System.loadLibrary("inventory");
    }

    // Native method declaration
    public native void processStock(Product product, int threshold);

    // Call site: Simulating the inventory check
    public static void main(String[] args) {
        InventoryManager manager = new InventoryManager();
        
        // Create a product with a low stock count
        Product widget = new Product("Industrial Widget", 15);
        int lowStockThreshold = 25;

        System.out.println("Before processing: " + widget.name + " stock is " + widget.stockCount);
        
        // Call the C JNI method to process the product
        manager.processStock(widget, lowStockThreshold);
        
        System.out.println("After processing: " + widget.name + " stock is " + widget.stockCount);
    }
}