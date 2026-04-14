public class PhysicsEngine {
    private double safetyZoneCenterX;
    private double safetyZoneCenterY;
    private double safetyZoneRadius;
    
    public PhysicsEngine(double centerX, double centerY, double radius) {
        this.safetyZoneCenterX = centerX;
        this.safetyZoneCenterY = centerY;
        this.safetyZoneRadius = radius;
    }
    
    public boolean isEntityInSafetyZone(Entity entity) {
        return entity.isWithinSafetyZone(safetyZoneCenterX, safetyZoneCenterY, safetyZoneRadius);
    }
    
    // Method to update safety zone parameters
    public void updateSafetyZone(double centerX, double centerY, double radius) {
        this.safetyZoneCenterX = centerX;
        this.safetyZoneCenterY = centerY;
        this.safetyZoneRadius = radius;
    }
}