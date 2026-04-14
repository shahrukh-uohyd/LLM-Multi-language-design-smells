/**
 * Drives the JNI constructor-assist demo.
 *
 * For each of the three Person constructors:
 *   1. Call the corresponding native method to locate the constructor
 *      and prepare/validate the data.
 *   2. Receive the prepared String[] from native code.
 *   3. Inspect the returned array and call the matching Person constructor.
 *   4. Use the constructed Person object.
 */
public class Main {

    public static void main(String[] args) {

        ConstructorAssistJNI jni = new ConstructorAssistJNI();

        System.out.println("====================================================");
        System.out.println("   JNI Constructor Assistance Demo                  ");
        System.out.println("====================================================\n");

        // ── Case 1: Full constructor ─────────────────────────────────────────
        System.out.println("[ Case 1 ] Full constructor (4 arguments)");
        System.out.println("  Raw input  : name='  alice smith ', age=29, " +
                           "email='ALICE@EXAMPLE.COM', role='senior developer'");

        String[] fullData = jni.prepareFullConstructor(
                "  alice smith ", 29, "ALICE@EXAMPLE.COM", "senior developer");

        System.out.println("  Native returned : " + java.util.Arrays.toString(fullData));

        Person p1 = buildPerson(fullData);
        System.out.println("  Constructed : " + p1);
        System.out.printf ("  Name length check (>0): %b%n", p1.name.length() > 0);
        System.out.printf ("  Age range   check (0-120): %b%n",
                            p1.age >= 0 && p1.age <= 120);
        System.out.println();

        // ── Case 2: Partial constructor ──────────────────────────────────────
        System.out.println("[ Case 2 ] Partial constructor (name + age)");
        System.out.println("  Raw input  : name='BOB JONES', age=45");

        String[] partialData = jni.preparePartialConstructor("BOB JONES", 45);

        System.out.println("  Native returned : " + java.util.Arrays.toString(partialData));

        Person p2 = buildPerson(partialData);
        System.out.println("  Constructed : " + p2);
        System.out.printf ("  Default email used: %b%n",
                            p2.email.equals("not-provided@example.com"));
        System.out.println();

        // ── Case 3: Default constructor ──────────────────────────────────────
        System.out.println("[ Case 3 ] Default constructor (no arguments)");

        String[] defaultData = jni.prepareDefaultConstructor();

        System.out.println("  Native returned : " + java.util.Arrays.toString(defaultData));

        Person p3 = buildPerson(defaultData);
        System.out.println("  Constructed : " + p3);
        System.out.printf ("  Is default person: %b%n", p3.name.equals("Unknown"));
        System.out.println();

        // ── Case 4: Invalid input handled by native layer ────────────────────
        System.out.println("[ Case 4 ] Invalid input — native layer sanitises data");
        System.out.println("  Raw input  : name='', age=200, " +
                           "email='not-an-email', role=''");

        String[] sanitisedData = jni.prepareFullConstructor(
                "", 200, "not-an-email", "");

        System.out.println("  Native returned : " + java.util.Arrays.toString(sanitisedData));

        Person p4 = buildPerson(sanitisedData);
        System.out.println("  Constructed : " + p4);
        System.out.printf ("  Age was clamped to valid range: %b%n",
                            p4.age >= 0 && p4.age <= 120);
        System.out.println();

        // ── Summary ──────────────────────────────────────────────────────────
        System.out.println("====================================================");
        System.out.println("  All constructed Person objects:");
        Person[] all = { p1, p2, p3, p4 };
        for (int i = 0; i < all.length; i++) {
            System.out.printf("  [%d] %s%n", i + 1, all[i]);
        }
        System.out.println("====================================================");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // buildPerson: inspects the array returned by native code and calls
    //              the matching Person constructor.
    //
    // Array layout:
    //   [0] tag   : "FULL" | "PARTIAL" | "DEFAULT"
    //   [1] name  : String
    //   [2] age   : String (numeric)
    //   [3] email : String
    //   [4] role  : String
    // ─────────────────────────────────────────────────────────────────────────
    private static Person buildPerson(String[] data) {
        if (data == null || data.length == 0) {
            System.out.println("  [WARN] Native returned null/empty — using default constructor.");
            return new Person();
        }

        String tag = data[0];

        switch (tag) {
            case "FULL": {
                String name  = data[1];
                int    age   = Integer.parseInt(data[2]);
                String email = data[3];
                String role  = data[4];
                return new Person(name, age, email, role);   // ← full constructor
            }
            case "PARTIAL": {
                String name = data[1];
                int    age  = Integer.parseInt(data[2]);
                return new Person(name, age);                // ← partial constructor
            }
            case "DEFAULT":
            default:
                return new Person();                         // ← default constructor
        }
    }
}