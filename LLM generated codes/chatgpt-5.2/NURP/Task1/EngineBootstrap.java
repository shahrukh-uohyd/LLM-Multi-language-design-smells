public class EngineBootstrap {

    static {
        PhysicsCoreLoader.load();
    }

    public static void main(String[] args) {
        // Safe to use native physics now
    }
}
