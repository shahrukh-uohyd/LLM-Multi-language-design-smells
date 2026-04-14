package com.jni.fields;

/**
 * Drives the JNI field-computation demo.
 *
 * Three Employee objects are created with different profiles.
 * For each one the native methods are called and their returned
 * values are printed and further processed in Java.
 */
public class Main {

    public static void main(String[] args) {

        FieldComputeJNI jni = new FieldComputeJNI();

        // ---- Create sample employees --------------------------------
        Employee[] employees = {
            new Employee("Alice Johnson", 40, 35.00, 0,  0.22),
            new Employee("Bob Martinez",  45, 28.50, 5,  0.18),
            new Employee("Carol Lee",     50, 52.00, 10, 0.30),
        };

        System.out.println("╔══════════════════════════════════════════════╗");
        System.out.println("║       JNI Employee Pay Computation Demo      ║");
        System.out.println("╚══════════════════════════════════════════════╝");

        double totalNetPay = 0.0;   // used after the loop – proves returned values are consumed

        for (Employee emp : employees) {

            System.out.println("\n" + emp);

            // --- Call native methods and use returned values ----------
            double gross   = jni.computeGrossPay(emp);
            double net     = jni.computeNetPay(emp);
            String summary = jni.computePaySummary(emp);

            // Use gross: classify pay band
            String band;
            if      (gross < 1000) band = "Band A (< $1,000)";
            else if (gross < 2000) band = "Band B ($1,000 – $1,999)";
            else                   band = "Band C ($2,000+)";

            System.out.printf("  Native gross pay  : $%,.2f  →  %s%n", gross, band);
            System.out.printf("  Native net pay    : $%,.2f%n", net);
            System.out.println("  Native pay summary:");
            // Indent every line of the multi-line summary
            for (String line : summary.split("\n")) {
                System.out.println("    " + line);
            }

            totalNetPay += net;   // accumulate returned value
        }

        System.out.println("\n──────────────────────────────────────────────");
        System.out.printf("  Combined net payroll (all employees): $%,.2f%n", totalNetPay);
        System.out.println("──────────────────────────────────────────────");
    }
}