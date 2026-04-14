/**
 * Java class that demonstrates the get-then-set pattern
 * Contains a field and a method that uses that field's value
 */
public class GetThenSetExample {
    private String name;
    private int count;

    public GetThenSetExample(String name, int count) {
        this.name = name;
        this.count = count;
    }

    // Method that will be called from native code with a field value
    public String processWithCount(String prefix) {
        return prefix + ": Name is " + name + ", Count was " + count;
    }

    // Getter methods for testing purposes
    public String getName() { return name; }
    public int getCount() { return count; }

    // Native method that implements the get-then-set pattern
    public native String getFieldAndInvokeMethod(GetThenSetExample obj);

    // Load the native library
    static {
        System.loadLibrary("getthenset");
    }

    @Override
    public String toString() {
        return "GetThenSetExample{name='" + name + "', count=" + count + "}";
    }

    public static void main(String[] args) {
        GetThenSetExample example = new GetThenSetExample("TestUser", 42);
        GetThenSetExample invoker = new GetThenSetExample("", 0);

        System.out.println("Original object: " + example);
        
        try {
            String result = invoker.getFieldAndInvokeMethod(example);
            System.out.println("Result from native method: " + result);
        } catch (Exception e) {
            System.err.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }
}