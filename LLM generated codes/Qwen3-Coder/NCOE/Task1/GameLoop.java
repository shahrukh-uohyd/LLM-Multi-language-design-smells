// GameLoop.java (Example usage)
public class GameLoop {
    private Entity playerEntity;
    private boolean running = true;
    
    public GameLoop() {
        this.playerEntity = new Entity(10.0f);
    }
    
    public void runGameLoop() {
        long targetFPS = 60;
        long frameTime = 1000 / targetFPS; // milliseconds per frame
        
        while (running) {
            long startTime = System.currentTimeMillis();
            
            // Game logic - update physics 60 times per second
            float newVelocity = calculateNewVelocity(); // Your physics calculation
            playerEntity.updateVelocityNative(newVelocity);
            
            // Render frame
            renderFrame();
            
            // Cap frame rate
            long endTime = System.currentTimeMillis();
            long sleepTime = frameTime - (endTime - startTime);
            
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    private float calculateNewVelocity() {
        // Your physics calculations here
        return (float) (Math.random() * 100); // Example calculation
    }
    
    private void renderFrame() {
        // Rendering code
        System.out.println("Current velocity: " + playerEntity.getVelocity());
    }
}