import java.util.ArrayList;
import java.util.List;

/**
 * Java class with several fields and methods to demonstrate object inspection through JNI
 */
public class ObjectInspector {
    private String name;
    private int age;
    private double salary;
    private boolean employed;
    private List<String> skills;

    public ObjectInspector(String name, int age, double salary, boolean employed, List<String> skills) {
        this.name = name;
        this.age = age;
        this.salary = salary;
        this.employed = employed;
        this.skills = skills != null ? new ArrayList<>(skills) : new ArrayList<>();
    }

    // Public getter methods
    public String getName() { return name; }
    public int getAge() { return age; }
    public double getSalary() { return salary; }
    public boolean isEmployed() { return employed; }
    public List<String> getSkills() { return new ArrayList<>(skills); }

    // Public setter methods
    public void setName(String name) { this.name = name; }
    public void setAge(int age) { this.age = age; }
    public void setSalary(double salary) { this.salary = salary; }
    public void setEmployed(boolean employed) { this.employed = employed; }
    public void addSkill(String skill) { this.skills.add(skill); }

    // A sample method
    public String getSummary() {
        return String.format("%s (%d years old) - Salary: %.2f, Employed: %b", 
                             name, age, salary, employed);
    }

    // Native method to inspect object fields
    public native String inspectFields(Object obj);
    
    // Native method to inspect object class information
    public native String inspectClass(Object obj);
    
    // Native method to get object's field values as a formatted string
    public native String getObjectDetails(Object obj);

    // Load the native library
    static {
        System.loadLibrary("objectinspector");
    }

    @Override
    public String toString() {
        return "ObjectInspector{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", salary=" + salary +
                ", employed=" + employed +
                ", skills=" + skills +
                '}';
    }

    public static void main(String[] args) {
        // Create an instance of ObjectInspector
        List<String> skillsList = new ArrayList<>();
        skillsList.add("Java");
        skillsList.add("Python");
        skillsList.add("JavaScript");
        
        ObjectInspector inspector = new ObjectInspector("Alice Johnson", 32, 95000.0, true, skillsList);
        ObjectInspector nativeInspector = new ObjectInspector("", 0, 0.0, false, null);

        System.out.println("Original object:");
        System.out.println(inspector);
        
        System.out.println("\nInspecting object using JNI:");
        System.out.println("Class Information:");
        System.out.println(nativeInspector.inspectClass(inspector));
        
        System.out.println("\nField Information:");
        System.out.println(nativeInspector.inspectFields(inspector));
        
        System.out.println("\nDetailed Object Information:");
        System.out.println(nativeInspector.getObjectDetails(inspector));
        
        // Test with another object
        ObjectInspector employee = new ObjectInspector("Bob Smith", 27, 78000.50, false, 
                                                     new ArrayList<>());
        employee.addSkill("C++");
        employee.addSkill("Rust");
        
        System.out.println("\nSecond object:");
        System.out.println(employee);
        System.out.println("\nDetailed Object Information:");
        System.out.println(nativeInspector.getObjectDetails(employee));
    }
}