/**
 * Java class with multiple properties to demonstrate JNI property access
 */
public class PropertyAccess {
    private String name;
    private int age;
    private double salary;
    private boolean employed;

    public PropertyAccess(String name, int age, double salary, boolean employed) {
        this.name = name;
        this.age = age;
        this.salary = salary;
        this.employed = employed;
    }

    // Native method declarations
    public native String getNameFromNative(PropertyAccess obj);
    public native int getAgeFromNative(PropertyAccess obj);
    public native double getSalaryFromNative(PropertyAccess obj);
    public native boolean getEmployedFromNative(PropertyAccess obj);

    // Load the native library
    static {
        System.loadLibrary("propertyaccess");
    }

    @Override
    public String toString() {
        return "PropertyAccess{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", salary=" + salary +
                ", employed=" + employed +
                '}';
    }

    public static void main(String[] args) {
        // Create an instance of PropertyAccess
        PropertyAccess person = new PropertyAccess("John Doe", 35, 75000.50, true);
        
        // Create an instance of the class to call native methods
        PropertyAccess accessor = new PropertyAccess("", 0, 0.0, false);
        
        System.out.println("Original object:");
        System.out.println(person);
        
        System.out.println("\nAccessing properties via JNI:");
        System.out.println("Name: " + accessor.getNameFromNative(person));
        System.out.println("Age: " + accessor.getAgeFromNative(person));
        System.out.println("Salary: " + accessor.getSalaryFromNative(person));
        System.out.println("Employed: " + accessor.getEmployedFromNative(person));
        
        // Test with another object
        PropertyAccess employee = new PropertyAccess("Jane Smith", 28, 68000.75, true);
        System.out.println("\nSecond object:");
        System.out.println(employee);
        System.out.println("Name: " + accessor.getNameFromNative(employee));
        System.out.println("Age: " + accessor.getAgeFromNative(employee));
        System.out.println("Salary: " + accessor.getSalaryFromNative(employee));
        System.out.println("Employed: " + accessor.getEmployedFromNative(employee));
    }
}