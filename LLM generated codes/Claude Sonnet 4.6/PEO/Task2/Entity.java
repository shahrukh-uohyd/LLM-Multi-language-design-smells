public class Entity {

    private int entityID;
    private byte[] meshData;        // sensitive / heavy — should never reach native safety check
    private String texturePath;     // irrelevant to boundary logic
    private double posX;
    private double posY;
    private double posZ;            // irrelevant — this is a 2D check on the XY plane

    public Entity(int entityID, byte[] meshData, String texturePath,
                  double posX, double posY, double posZ) {
        this.entityID   = entityID;
        this.meshData   = meshData;
        this.texturePath = texturePath;
        this.posX       = posX;
        this.posY       = posY;
        this.posZ       = posZ;
    }

    /**
     * APPROACH A — Pass the full object; native code must self-restrict to posX/posY.
     * Less safe: native code structurally has access to meshData, texturePath, etc.
     */
    public native boolean isInSafetyZone(double zoneCenterX, double zoneCenterY, double zoneRadius);

    /**
     * APPROACH B (RECOMMENDED) — Pass only the two doubles needed.
     * Native code never receives a reference to this object.
     * meshData, texturePath, posZ are structurally inaccessible.
     */
    public native boolean isInSafetyZoneSecure(double posX, double posY,
                                               double zoneCenterX, double zoneCenterY,
                                               double zoneRadius);

    static {
        System.loadLibrary("physics_engine");
    }
}