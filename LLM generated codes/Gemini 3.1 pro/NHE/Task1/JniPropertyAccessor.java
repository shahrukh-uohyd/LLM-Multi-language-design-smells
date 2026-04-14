public class JniPropertyAccessor {
    
    // Load the native shared library
    static {
        System.loadLibrary("propertyaccessor"); 
    }

    // Native method that receives an instance of the Person class
    // and returns a String (the extracted property)
    public native String getPersonName(Person person);

    public static void main(String[] args) {
        JniPropertyAccessor accessor = new JniPropertyAccessor();
        
        // Create an instance of the class
        Person person = new Person("Alice", 30);
        
        System.out.println("[Java] Passing Person object to native method...");
        
        // The native method retrieves the 'name' property and returns it
        String retrievedName = accessor.getPersonName(person);
        
        // The retrieved value is returned to Java and used
        System.out.println("[Java] Native method successfully extracted and returned the name: " + retrievedName);
    }
}