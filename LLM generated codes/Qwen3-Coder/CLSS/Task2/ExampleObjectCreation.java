public class ExampleObjectCreation {
    public static void main(String[] args) {
        NativeObjectCreator creator = new NativeObjectCreator();
        
        // Create a new DataContainer from native code
        DataContainer container = creator.createDataContainer(3.14f);
        
        if (container != null) {
            System.out.println("Created container: " + container);
            System.out.println("Value: " + container.getValue());
        } else {
            System.out.println("Failed to create container");
        }
    }
}