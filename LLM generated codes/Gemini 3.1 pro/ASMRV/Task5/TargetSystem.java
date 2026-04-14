// A Java class whose members will be accessed indirectly via JNI
public class TargetSystem {
    // Private field
    private int systemId = 101;
    
    // Protected field
    protected String systemName = "AlphaNode";

    // Private method
    private void initializeSystem() {
        System.out.println("System initialized.");
    }
    
    // Public method
    public int getSystemId() {
        return systemId;
    }
}