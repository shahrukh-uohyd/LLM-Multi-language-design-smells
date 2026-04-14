public class Main {

    public static void main(String[] args) {

        System.out.println("=== JNI Field-to-Method Demo ===");
        System.out.println();

        // ── Normal case ──────────────────────────────────────────
        Account acc = new Account("Alice", 10000.00, 0.05, true);
        System.out.println("Before : " + acc);

        NativeBridge bridge = new NativeBridge();
        double interest = bridge.computeAndApplyInterest(acc);

        System.out.printf("Interest returned to Java : %.2f%n", interest);
        System.out.println("After  : " + acc);
        System.out.println();

        // ── Inactive account edge case ────────────────────────────
        System.out.println("--- Edge case: inactive account ---");
        Account inactive = new Account("Bob", 5000.00, 0.03, false);
        System.out.println("Before : " + inactive);

        double interest2 = bridge.computeAndApplyInterest(inactive);
        System.out.printf("Interest returned to Java : %.2f%n", interest2);
        System.out.println("After  : " + inactive);
        System.out.println();

        // ── Zero balance edge case ────────────────────────────────
        System.out.println("--- Edge case: zero balance ---");
        Account zeroAcc = new Account("Carol", 0.00, 0.07, true);
        System.out.println("Before : " + zeroAcc);

        double interest3 = bridge.computeAndApplyInterest(zeroAcc);
        System.out.printf("Interest returned to Java : %.2f%n", interest3);
        System.out.println("After  : " + zeroAcc);
    }
}