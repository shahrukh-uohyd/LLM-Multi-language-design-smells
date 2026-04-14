/**
 * Java class used in the array processing example
 */
class DataObject {
    private String id;
    private int value;

    public DataObject(String id, int value) {
        this.id = id;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public int getValue() {
        return value;
    }

    public String process() {
        return "Processed: " + id + " with value " + value;
    }

    @Override
    public String toString() {
        return "DataObject{id='" + id + "', value=" + value + "}";
    }
}

/**
 * Main class that demonstrates upcall inside a loop
 */
public class LoopUpcallExample {
    // Native method that processes an array of objects
    public native String[] processObjects(DataObject[] objects);

    // Load the native library
    static {
        System.loadLibrary("loopupcall");
    }

    public static void main(String[] args) {
        // Create an array of DataObject instances
        DataObject[] objects = {
            new DataObject("obj1", 10),
            new DataObject("obj2", 20),
            new DataObject("obj3", 30),
            new DataObject("obj4", 40)
        };

        System.out.println("Processing objects:");
        for (DataObject obj : objects) {
            System.out.println("  " + obj);
        }

        LoopUpcallExample processor = new LoopUpcallExample();
        
        try {
            String[] results = processor.processObjects(objects);
            System.out.println("\nResults from native processing:");
            for (int i = 0; i < results.length; i++) {
                System.out.println("  [" + i + "] " + results[i]);
            }
        } catch (Exception e) {
            System.err.println("Error during native processing: " + e.getMessage());
            e.printStackTrace();
        }
    }
}