public class Person {
    // Multiple properties
    private String name;
    private int age;

    public Person(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // Standard getters (Note: JNI can access private fields directly, bypassing getters)
    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }
}