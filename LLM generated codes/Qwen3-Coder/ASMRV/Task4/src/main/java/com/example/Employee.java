package com.example;

public class Employee extends Person {
    private String employeeId;
    private String department;
    private double salary;
    
    // Default constructor
    public Employee() {
        super();
        this.employeeId = "EMP000";
        this.department = "General";
        this.salary = 0.0;
    }
    
    // Constructor with name and employee details
    public Employee(String firstName, String lastName, String employeeId, String department) {
        super(firstName, lastName);
        this.employeeId = employeeId;
        this.department = department;
        this.salary = 50000.0; // Default salary
    }
    
    // Full constructor
    public Employee(String firstName, String lastName, int age, String email, 
                   String employeeId, String department, double salary) {
        super(firstName, lastName, age, email);
        this.employeeId = employeeId;
        this.department = department;
        this.salary = salary;
    }
    
    // Copy constructor
    public Employee(Employee other) {
        super(other);
        this.employeeId = other.employeeId;
        this.department = other.department;
        this.salary = other.salary;
    }
    
    // Getters
    public String getEmployeeId() { return employeeId; }
    public String getDepartment() { return department; }
    public double getSalary() { return salary; }
    
    // Setters
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    public void setDepartment(String department) { this.department = department; }
    public void setSalary(double salary) { this.salary = salary; }
    
    @Override
    public String toString() {
        return String.format("Employee{employeeId='%s', department='%s', salary=%.2f, %s}", 
                           employeeId, department, salary, super.toString());
    }
    
    @Override
    public String getFullName() {
        return super.getFullName() + " (Employee)";
    }
}