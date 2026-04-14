public class Main {

    public static void main(String[] args) {

        Employee emp = new Employee("Diana Prince", 1042,
                                    "Engineering", 88500.50, true);

        System.out.println("=== JNI Object Inspection Demo ===");
        System.out.println("Java object  : " + emp);
        System.out.println();

        NativeBridge bridge = new NativeBridge();
        String report = bridge.inspect(emp);

        System.out.println("--- Inspection report (built in native C code) ---");
        System.out.println(report);

        // Verify each field using Java-side values
        System.out.println("--- Java-side verification ---");
        System.out.println("name       : " + emp.name);
        System.out.println("id         : " + emp.id);
        System.out.println("department : " + emp.department);
        System.out.println("salary     : " + emp.salary);
        System.out.println("active     : " + emp.active);
    }
}