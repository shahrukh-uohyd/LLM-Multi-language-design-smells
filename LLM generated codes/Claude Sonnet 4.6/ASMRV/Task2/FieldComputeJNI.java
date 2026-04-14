package com.jni.fields;

/**
 * Exposes three native methods, each reading fields of an Employee
 * object and returning a computed value back to Java.
 *
 *  computeGrossPay   – base pay + overtime premium
 *  computeNetPay     – gross pay minus taxes
 *  computePaySummary – full formatted summary string
 */
public class FieldComputeJNI {

    static {
        System.loadLibrary("FieldComputeJNI");
    }

    /**
     * Reads hoursWorked, hourlyRate, and overtimeHours from the
     * Employee object and returns gross pay.
     *
     * Formula:
     *   regularPay  = hoursWorked * hourlyRate
     *   overtimePay = overtimeHours * hourlyRate * 1.5
     *   grossPay    = regularPay + overtimePay
     */
    public native double computeGrossPay(Employee emp);

    /**
     * Reads grossPay (computed internally) and taxRate from the
     * Employee object and returns net pay after tax deduction.
     *
     * Formula:
     *   netPay = grossPay * (1.0 - taxRate)
     */
    public native double computeNetPay(Employee emp);

    /**
     * Returns a complete formatted pay-slip string built entirely
     * inside the native layer by reading all Employee fields.
     */
    public native String computePaySummary(Employee emp);
}