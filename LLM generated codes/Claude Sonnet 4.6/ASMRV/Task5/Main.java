/**
 * Demonstrates JNI reflection-like operations.
 *
 * For each inspected member the returned String[] is consumed by Java:
 *   - information is printed in a formatted table
 *   - the found-flag is checked before use
 *   - getter results are used in a runtime summary
 */
public class Main {

    public static void main(String[] args) {

        ReflectJNI jni = new ReflectJNI();

        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║         JNI Reflection-Like Operations Demo          ║");
        System.out.println("╚══════════════════════════════════════════════════════╝");

        // ── Section 1: Inspect Fields ────────────────────────────────────────
        System.out.println("\n─── Field Inspection ───────────────────────────────────");
        printMemberHeader();

        String[] fieldNames = { "brand", "year", "engineCapacity", "isElectric",
                                "speed", "nonExistentField" };

        for (String fn : fieldNames) {
            String[] info = jni.inspectField(fn);    // ← call native method
            consumeMemberInfo(info);                  // ← use returned value
        }

        // ── Section 2: Inspect Methods ───────────────────────────────────────
        System.out.println("\n─── Method Inspection ──────────────────────────────────");
        printMemberHeader();

        // Each entry: { methodName, descriptor }
        String[][] methods = {
            { "getBrand",          "()Ljava/lang/String;" },
            { "getYear",           "()I"                  },
            { "accelerate",        "(I)V"                 },
            { "getEngineCapacity", "()D"                  },
            { "checkElectric",     "()Z"                  },
            { "describe",          "()Ljava/lang/String;" },
            { "nonExistentMethod", "()V"                  },
        };

        for (String[] m : methods) {
            String[] info = jni.inspectMethod(m[0], m[1]);  // ← call native method
            consumeMemberInfo(info);                          // ← use returned value
        }

        // ── Section 3: Invoke Getters on a Live Object ───────────────────────
        System.out.println("\n─── Getter Invocation on a Live Vehicle Object ─────────");

        Vehicle car = new Vehicle("Tesla Model S", 2023, 0.0, true, 0);
        System.out.println("  Vehicle instance: " + car);
        System.out.println();

        String[][] getters = {
            { "getBrand", "()Ljava/lang/String;" },
            { "getYear",  "()I"                  },
        };

        for (String[] g : getters) {
            String[] result = jni.invokeGetter(car, g[0], g[1]); // ← call native
            consumeInvokeResult(result, car);                      // ← use returned value
        }

        // ── Section 4: Build a runtime summary using all returned values ─────
        System.out.println("\n─── Runtime Summary Built from Returned Values ─────────");
        buildRuntimeSummary(jni, car);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Consume and display the String[] returned by inspectField / inspectMethod
    // ─────────────────────────────────────────────────────────────────────────
    private static void consumeMemberInfo(String[] info) {
        if (info == null || info.length < 6) {
            System.out.printf("  %-20s %-10s %-35s %-12s %-10s%n",
                    "?", "?", "?", "?", "NOT FOUND");
            return;
        }

        // Use the found flag from slot [5]
        boolean found = "true".equals(info[5]);
        String status = found ? "OK" : "NOT FOUND";

        System.out.printf("  %-20s %-10s %-35s %-12s %-10s%n",
                info[1],   // name
                info[0],   // kind (FIELD / METHOD)
                info[2],   // JVM descriptor
                info[3],   // human type / return type
                status
        );

        // Further use: warn about private members
        if (found && "private".equals(info[4])) {
            System.out.printf("  %s  ↑ NOTE: '%s' is private — direct access restricted%n",
                    " ".repeat(20), info[1]);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Consume the String[] returned by invokeGetter
    // ─────────────────────────────────────────────────────────────────────────
    private static void consumeInvokeResult(String[] result, Vehicle v) {
        if (result == null || result.length < 6) {
            System.out.println("  [INVOKE] No result returned.");
            return;
        }
        boolean found = "true".equals(result[5]);
        if (!found) {
            System.out.printf("  [INVOKE] Method '%s' not found.%n", result[1]);
            return;
        }
        // Use returned method name, return type, and result value
        System.out.printf("  [INVOKE] %s() → (%s) \"%s\"%n",
                result[1],   // method name
                result[2],   // return type
                result[3]    // result value as string
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Build and print a runtime summary by combining all returned values
    // ─────────────────────────────────────────────────────────────────────────
    private static void buildRuntimeSummary(ReflectJNI jni, Vehicle car) {
        // Inspect the 'brand' field and the 'getYear' method
        String[] brandField  = jni.inspectField("brand");
        String[] yearField   = jni.inspectField("year");
        String[] brandGetter = jni.inspectMethod("getBrand", "()Ljava/lang/String;");
        String[] yearGetter  = jni.invokeGetter(car, "getYear", "()I");

        System.out.println("  Summary table of consumed return values:");
        System.out.println("  ┌────────────────────────┬──────────────────────────┐");
        System.out.printf ("  │ %-22s │ %-24s │%n", "Item", "Value");
        System.out.println("  ├────────────────────────┼──────────────────────────┤");

        printSummaryRow("brand field type",
                brandField  != null ? brandField[3]  : "N/A");
        printSummaryRow("brand field modifier",
                brandField  != null ? brandField[4]  : "N/A");
        printSummaryRow("year field type",
                yearField   != null ? yearField[3]   : "N/A");
        printSummaryRow("getBrand descriptor",
                brandGetter != null ? brandGetter[2] : "N/A");
        printSummaryRow("getBrand return type",
                brandGetter != null ? brandGetter[3] : "N/A");
        printSummaryRow("getYear() live result",
                yearGetter  != null ? yearGetter[3]  : "N/A");

        System.out.println("  └────────────────────────┴──────────────────────────┘");
    }

    private static void printSummaryRow(String label, String value) {
        System.out.printf("  │ %-22s │ %-24s │%n", label, value);
    }

    private static void printMemberHeader() {
        System.out.printf("  %-20s %-10s %-35s %-12s %-10s%n",
                "Name", "Kind", "JVM Descriptor", "Type/Return", "Status");
        System.out.println("  " + "─".repeat(90));
    }
}