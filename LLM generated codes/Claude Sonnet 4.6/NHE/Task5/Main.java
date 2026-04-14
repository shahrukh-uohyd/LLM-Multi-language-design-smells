public class Main {

    public static void main(String[] args) {

        Product[] products = {
            new Product("Laptop",     999.99,  5, true),
            new Product("Mouse",       19.99, 50, true),
            new Product("Monitor",    349.99,  8, true),
            new Product("Keyboard",    49.99, 30, false),
            new Product("Webcam",      79.99,  0, false)
        };

        System.out.println("=== JNI Array Iteration Demo ===");
        System.out.println("Products passed to native code:");
        for (Product p : products) {
            System.out.println("  " + p);
        }
        System.out.println();

        NativeBridge bridge  = new NativeBridge();
        double[]     results = bridge.processProducts(products);

        System.out.println("--- Results returned from native C++ code ---");
        double grandTotal = 0.0;
        for (int i = 0; i < results.length; i++) {
            System.out.printf("  %-12s → getTotalValue() = %.2f%n",
                              products[i].getName(), results[i]);
            grandTotal += results[i];
        }
        System.out.printf("%nGrand total (all products) : %.2f%n", grandTotal);

        // Java-side verification
        System.out.println();
        System.out.println("--- Java-side verification ---");
        boolean allMatch = true;
        for (int i = 0; i < products.length; i++) {
            double expected = products[i].getTotalValue();
            if (Double.compare(expected, results[i]) != 0) {
                System.out.printf("  MISMATCH at index %d: expected %.2f, got %.2f%n",
                                  i, expected, results[i]);
                allMatch = false;
            }
        }
        if (allMatch) {
            System.out.println("  SUCCESS: all values match.");
        }
    }
}