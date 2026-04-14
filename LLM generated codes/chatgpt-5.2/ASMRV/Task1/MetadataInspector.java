// File: MetadataInspector.java
public class MetadataInspector {

    static {
        System.loadLibrary("metadata_inspector");
    }

    // Native method declaration
    public native String getClassMetadata(String className);

    public static void main(String[] args) {
        MetadataInspector inspector = new MetadataInspector();

        String metadata = inspector.getClassMetadata("TargetClass");
        System.out.println("Retrieved metadata:");
        System.out.println(metadata);
    }
}
