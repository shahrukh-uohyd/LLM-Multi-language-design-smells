public class Main {

    public static void main(String[] args) {

        Person person = new Person("Alice Johnson", 30, "alice@example.com", 95000.00);

        System.out.println("=== JNI Property Access Demo ===");
        System.out.println("Java object created : " + person);

        NativeBridge bridge = new NativeBridge();
        String nameFromNative = bridge.getPersonName(person);

        System.out.println("\nName retrieved by native C code : \"" + nameFromNative + "\"");

        if (person.name.equals(nameFromNative)) {
            System.out.println("SUCCESS: Value matches. JNI access is working correctly.");
        } else {
            System.out.println("FAILURE: Mismatch detected.");
        }
    }
}