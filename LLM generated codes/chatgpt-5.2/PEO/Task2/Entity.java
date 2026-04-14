public class Entity {

    private int entityID;
    private byte[] meshData;
    private String texturePath;

    // Position in 3D space
    private double posX;
    private double posY;
    private double posZ;

    static {
        System.loadLibrary("physics");
    }

    public Entity(int entityID,
                  byte[] meshData,
                  String texturePath,
                  double posX,
                  double posY,
                  double posZ) {
        this.entityID = entityID;
        this.meshData = meshData;
        this.texturePath = texturePath;
        this.posX = posX;
        this.posY = posY;
        this.posZ = posZ;
    }

    /**
     * Native check: is this entity inside a circular 2D safety zone?
     *
     * @param centerX  safety zone center X
     * @param centerY  safety zone center Y
     * @param radius   safety zone radius
     */
    public native boolean isWithinSafetyZone(double centerX,
                                             double centerY,
                                             double radius);

    // Demo
    public static void main(String[] args) {
        Entity e = new Entity(
            1,
            new byte[]{1, 2, 3},
            "mesh.png",
            3.0,
            4.0,
            10.0
        );

        boolean safe = e.isWithinSafetyZone(0.0, 0.0, 5.0);
        System.out.println("Entity within safety zone: " + safe);
    }
}
