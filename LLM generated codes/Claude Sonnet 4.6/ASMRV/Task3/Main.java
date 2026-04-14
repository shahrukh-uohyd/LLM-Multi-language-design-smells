/**
 * Demonstrates the JNI field-access workflow:
 *   1. Create Product objects in Java.
 *   2. Pass each one to the native method.
 *   3. Receive the retrieved integer value back.
 *   4. Use the returned value in Java logic.
 */
public class Main {

    public static void main(String[] args) {

        FieldAccessJNI accessor = new FieldAccessJNI();

        // Create several Product objects with different quantities
        Product[] products = {
            new Product("Laptop",      15,  999.99, "Electronics"),
            new Product("Coffee Mug",  200,   8.49, "Kitchen"),
            new Product("Desk Chair",    3, 349.00, "Furniture"),
            new Product("Notebook",    500,   2.99, "Stationery"),
        };

        System.out.println("=================================================");
        System.out.println("   JNI Integer Field Retrieval Demo              ");
        System.out.println("=================================================\n");

        for (Product p : products) {
            System.out.println("Java object  : " + p);

            // ── Call the native method ──────────────────────────────
            int retrievedQty = accessor.getQuantity(p);
            // ───────────────────────────────────────────────────────

            System.out.println("Native result: quantity field = " + retrievedQty);

            // Use the returned integer value: determine stock status
            String stockStatus;
            if      (retrievedQty == 0)  stockStatus = "OUT OF STOCK";
            else if (retrievedQty  < 10) stockStatus = "LOW STOCK";
            else if (retrievedQty  < 100)stockStatus = "IN STOCK";
            else                         stockStatus = "BULK STOCK";

            System.out.println("Stock status : " + stockStatus);
            System.out.println("-------------------------------------------------");
        }
    }
}