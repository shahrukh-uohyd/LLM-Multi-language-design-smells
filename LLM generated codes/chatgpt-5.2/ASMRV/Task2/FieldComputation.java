// File: FieldComputation.java
public class FieldComputation {

    static {
        System.loadLibrary("field_computation");
    }

    // Native method that computes a value derived from Employee fields
    public native double computeTotalEarnings(Employee employee);

    public static void main(String[] args) {
        Employee emp = new Employee(5, 75000.0);

        FieldComputation fc = new FieldComputation();
        double total = fc.computeTotalEarnings(emp);

        System.out.println("Computed total earnings: " + total);
    }
}
