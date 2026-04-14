package com.jni.fields;

/**
 * Represents an employee with several numeric and string fields.
 * The native layer will read these fields and compute derived values.
 */
public class Employee {

    public String name;
    public int    hoursWorked;      // per week
    public double hourlyRate;       // in USD
    public int    overtimeHours;    // hours beyond 40
    public double taxRate;          // e.g. 0.25 means 25%

    public Employee(String name,
                    int    hoursWorked,
                    double hourlyRate,
                    int    overtimeHours,
                    double taxRate) {
        this.name          = name;
        this.hoursWorked   = hoursWorked;
        this.hourlyRate    = hourlyRate;
        this.overtimeHours = overtimeHours;
        this.taxRate       = taxRate;
    }

    @Override
    public String toString() {
        return String.format(
            "Employee{ name='%s', hoursWorked=%d, hourlyRate=%.2f, " +
            "overtimeHours=%d, taxRate=%.2f }",
            name, hoursWorked, hourlyRate, overtimeHours, taxRate);
    }
}