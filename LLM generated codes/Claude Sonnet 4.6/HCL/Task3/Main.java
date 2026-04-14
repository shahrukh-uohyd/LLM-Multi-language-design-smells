/**
 * Application entry point – demonstrates library loading and graceful
 * error handling if no library variant can be found.
 */
public class Main {

    public static void main(String[] args) {
        try {
            NativeGraphicsService graphics = new NativeGraphicsService();
            graphics.selfTest();

        } catch (GraphicsLibraryLoadException e) {
            // Printed message already contains the full diagnostic report.
            System.err.println("FATAL: " + e.getMessage());
            System.exit(1);

        } catch (AssertionError e) {
            System.err.println("Self-test failed: " + e.getMessage());
            System.exit(2);
        }
    }
}