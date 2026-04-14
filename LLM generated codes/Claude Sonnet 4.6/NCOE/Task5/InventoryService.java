import java.util.List;

public class InventoryService {

    // ------------------------------------------------------------------
    // Policy constants — live in Java so they are testable and
    // configurable without touching or recompiling any native code.
    // ------------------------------------------------------------------
    public static final int    LOW_STOCK_THRESHOLD  = 10;
    public static final int    REORDER_CYCLE_DAYS   = 30;
    public static final double SAFETY_STOCK_FACTOR  = 1.5;
    public static final int    MIN_ORDER_QUANTITY   = 20;

    static {
        System.loadLibrary("inventoryservice");
    }

    // ------------------------------------------------------------------
    // Native API — strictly limited to field I/O, no business logic
    // ------------------------------------------------------------------

    /**
     * One-time initialisation: resolves and caches the {@code jfieldID}
     * for {@code Product.stockCount} on the C side.
     *
     * <p>Must be called once on the main thread before any inventory
     * processing begins. Passing {@code Product.class} directly from Java
     * avoids the {@code FindClass} / bootstrap-classloader pitfall that
     * causes a silent {@code null} return on background threads.
     *
     * @param productClass      pass {@code Product.class} explicitly
     * @param lowStockThreshold the minimum acceptable stock level;
     *                          stored on the C side so the policy value
     *                          travels with the cache, not as a magic literal
     */
    public static native void initNativeCache(Class<?> productClass,
                                              int lowStockThreshold);

    /**
     * Reads {@code product.stockCount} using the cached field ID.
     *
     * <p>This is the <em>only</em> responsibility of this native method:
     * reading one integer field. No threshold logic, no calculation.
     *
     * @param  product the {@link Product} to read from
     * @return         the current value of {@code stockCount}
     */
    public static native int readStockCount(Object product);

    /**
     * Writes {@code newStock} into {@code product.stockCount} using the
     * cached field ID.
     *
     * <p>This is the <em>only</em> responsibility of this native method:
     * writing one integer field. No threshold logic, no calculation.
     *
     * @param product  the {@link Product} to update
     * @param newStock the new stock level to set
     */
    public static native void writeStockCount(Object product, int newStock);

    // ------------------------------------------------------------------
    // Business logic — pure Java, fully unit-testable, JIT-optimisable
    // ------------------------------------------------------------------

    /**
     * Calculates the replenishment quantity for a product that has fallen
     * below the low-stock threshold.
     *
     * <p>Formula:
     * <pre>
     *   dailyUsageRate    = currentStock / REORDER_CYCLE_DAYS
     *   reorderQty        = dailyUsageRate * REORDER_CYCLE_DAYS * SAFETY_STOCK_FACTOR
     *   reorderQty        = max(reorderQty, MIN_ORDER_QUANTITY)
     *   newStock          = currentStock + reorderQty
     * </pre>
     *
     * <p>Living in Java means this formula can be unit tested with JUnit,
     * changed by the product team without touching C, and profiled by the JVM.
     *
     * @param  currentStock the stock level at the time the reorder is triggered
     * @return              the replenishment quantity to add
     */
    public static int calculateReplenishmentQty(int currentStock) {
        double dailyUsageRate = (double) currentStock / REORDER_CYCLE_DAYS;
        int    reorderQty     = (int) Math.ceil(
                                    dailyUsageRate * REORDER_CYCLE_DAYS
                                    * SAFETY_STOCK_FACTOR);

        return Math.max(reorderQty, MIN_ORDER_QUANTITY);
    }

    // ------------------------------------------------------------------
    // Orchestration — wires the native field I/O to the Java logic
    // ------------------------------------------------------------------

    /**
     * Checks whether {@code product} needs restocking and, if so,
     * calculates and applies the replenishment quantity.
     *
     * <p>The orchestration flow is deliberately expressed here in Java
     * rather than inside a single "do everything" native function:
     * <ol>
     *   <li>Read the field via native I/O (no logic).</li>
     *   <li>Apply threshold and calculation in pure Java (no JNI).</li>
     *   <li>Write the result via native I/O (no logic).</li>
     * </ol>
     *
     * @param  product the {@link Product} to evaluate and potentially restock
     * @return         {@code true} if a restock was applied
     */
    public static boolean checkAndRestock(Product product) {
        // ✅ Step 1 — Read: one native call, one field, no logic in C.
        int currentStock = readStockCount(product);

        if (currentStock >= LOW_STOCK_THRESHOLD) {
            // Stock is healthy — nothing to do.
            return false;
        }

        // ✅ Step 2 — Calculate: pure Java, fully testable, JIT-compiled.
        int replenishmentQty = calculateReplenishmentQty(currentStock);
        int newStock         = currentStock + replenishmentQty;

        System.out.printf(
            "  [restock] %-12s stock=%2d < threshold=%d → +%d → newStock=%d%n",
            product.productId, currentStock,
            LOW_STOCK_THRESHOLD, replenishmentQty, newStock);

        // ✅ Step 3 — Write: one native call, one field, no logic in C.
        //    Uses the SAME cached jfieldID as the read in Step 1.
        //    No second GetFieldID call — ever.
        writeStockCount(product, newStock);

        return true;
    }

    // ------------------------------------------------------------------
    // Batch processing call site
    // ------------------------------------------------------------------

    /**
     * Processes a list of products, restocking any that are below threshold.
     *
     * @param products the inventory list to evaluate
     */
    public static void processBatch(List<Product> products) {
        System.out.printf("%n--- Processing inventory batch of %d products ---%n",
                          products.size());

        int restockedCount = 0;
        for (Product product : products) {
            if (checkAndRestock(product)) {
                restockedCount++;
            }
        }

        System.out.printf("--- Batch complete: %d/%d products restocked ---%n%n",
                          restockedCount, products.size());
    }

    // ------------------------------------------------------------------
    // Entry point
    // ------------------------------------------------------------------

    public static void main(String[] args) {
        // ✅ Step 1 — Resolve and cache the field ID once, on the main
        //    thread, before any product is processed.
        initNativeCache(Product.class, LOW_STOCK_THRESHOLD);

        // ✅ Step 2 — Build a representative inventory set.
        List<Product> inventory = List.of(
            new Product("SKU-001", "Widget A",   3,  4.99),  // below threshold
            new Product("SKU-002", "Widget B",  25,  9.99),  // healthy
            new Product("SKU-003", "Gadget C",   0, 14.99),  // zero stock
            new Product("SKU-004", "Gadget D",  10,  2.49),  // exactly at threshold
            new Product("SKU-005", "Component E", 7, 19.99), // below threshold
            new Product("SKU-006", "Component F",50,  1.99)  // healthy
        );

        System.out.println("Before processing:");
        inventory.forEach(p -> System.out.println("  " + p));

        // ✅ Step 3 — Process the batch. The loop and all logic live in Java.
        processBatch(inventory);

        System.out.println("After processing:");
        inventory.forEach(p -> System.out.println("  " + p));
    }
}