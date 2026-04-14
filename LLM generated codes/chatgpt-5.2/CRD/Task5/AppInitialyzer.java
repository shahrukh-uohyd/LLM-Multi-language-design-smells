public class AppInitializer {

    private final SystemMonitor monitor = new SystemMonitor();

    public void initialize(String configFile) {
        // New native configuration feature used only here
        monitor.loadConfiguration(configFile);
        System.out.println("Configuration loaded");

        // Existing monitoring functionality
        monitor.printSystemStatus();
    }

    public static void main(String[] args) {
        AppInitializer initializer = new AppInitializer();
        initializer.initialize("app.conf");
    }
}
