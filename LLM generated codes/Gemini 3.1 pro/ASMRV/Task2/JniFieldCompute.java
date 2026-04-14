public class JniFieldCompute {
    
    // Load the shared C library at runtime
    static {
        System.loadLibrary("fieldcompute");
    }

    // Native method declaration: takes an object, inspects its fields, and returns a computed double
    public native double computeTotalValue(InventoryItem item);

    public static void main(String[] args) {
        JniFieldCompute app = new JniFieldCompute();
        
        // Create an instance of our class with multiple fields
        InventoryItem item = new InventoryItem("Wireless Keyboard", 15, 29.99);
        
        System.out.println("Item: " + item.name);
        System.out.println("Quantity: " + item.quantity);
        System.out.println("Price per unit: $" + item.price);
        System.out.println("-------------------------------------------------");
        System.out.println("Calling native method to compute total value...");
        
        // The native method is invoked to compute a value derived from the object's fields
        double totalValue = app.computeTotalValue(item);
        
        // The returned value is used by the Java application
        System.out.printf("Result retrieved from JNI Native Code: $%.2f%n", totalValue);
        System.out.println("-------------------------------------------------");
    }
}