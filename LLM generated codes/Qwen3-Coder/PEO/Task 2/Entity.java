public class Entity {
    private int entityID;
    private String meshData;      // Could be path to mesh file or serialized mesh data
    private String texturePath;
    private double posX;
    private double posY;
    private double posZ;

    // Constructor
    public Entity(int entityID, String meshData, String texturePath, 
                  double posX, double posY, double posZ) {
        this.entityID = entityID;
        this.meshData = meshData;
        this.texturePath = texturePath;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }

    // Getters
    public int getEntityID() { return entityID; }
    public String getMeshData() { return meshData; }
    public String getTexturePath() { return texturePath; }
    public double getPosX() { return posX; }
    public double getPosY() { return posY; }
    public double getPosZ() { return posZ; }

    // Native method declaration - checks if entity is within safety zone
    // Parameters: centerX, centerY, radius
    public native boolean isWithinSafetyZone(double centerX, double centerY, double radius);

    // Static block to load the native library
    static {
        System.loadLibrary("physics_engine"); // loads libphysics_engine.so (Linux/Mac) or physics_engine.dll (Windows)
    }
}