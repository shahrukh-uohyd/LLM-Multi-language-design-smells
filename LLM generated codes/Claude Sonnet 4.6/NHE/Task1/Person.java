/**
 * A Java class representing a Person with multiple properties.
 * An instance of this class will be passed to native code via JNI.
 */
public class Person {

    private String name;
    private int age;
    private String email;
    private double salary;

    public Person(String name, int age, String email, double salary) {
        this.name   = name;
        this.age    = age;
        this.email  = email;
        this.salary = salary;
    }

    // Getters
    public String getName()   { return name;   }
    public int    getAge()    { return age;     }
    public String getEmail()  { return email;   }
    public double getSalary() { return salary;  }

    @Override
    public String toString() {
        return "Person{name='" + name + "', age=" + age
             + ", email='" + email + "', salary=" + salary + "}";
    }
}