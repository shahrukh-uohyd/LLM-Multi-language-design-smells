/**
 * Represents a person with multiple constructors.
 * The native layer will locate a constructor, prepare and validate
 * the field data needed, and return it to Java for object construction.
 */
public class Person {

    public String name;
    public int    age;
    public String email;
    public String role;

    // ── Constructor 1: full details ───────────────────────────────��──
    public Person(String name, int age, String email, String role) {
        this.name  = name;
        this.age   = age;
        this.email = email;
        this.role  = role;
    }

    // ── Constructor 2: name and age only (defaults for email/role) ───
    public Person(String name, int age) {
        this.name  = name;
        this.age   = age;
        this.email = "not-provided@example.com";
        this.role  = "Guest";
    }

    // ── Constructor 3: default / empty person ────────────────────────
    public Person() {
        this.name  = "Unknown";
        this.age   = 0;
        this.email = "unknown@example.com";
        this.role  = "None";
    }

    @Override
    public String toString() {
        return String.format(
            "Person{ name='%s', age=%d, email='%s', role='%s' }",
            name, age, email, role
        );
    }
}