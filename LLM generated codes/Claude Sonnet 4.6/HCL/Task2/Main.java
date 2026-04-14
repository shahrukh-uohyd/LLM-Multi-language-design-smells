/**
 * Entry point – demonstrates explicit library loading and engine usage.
 */
public class Main {

    public static void main(String[] args) {
        try {
            // Option A – automatic (static initialiser in SimEngineLoader fires here)
            SimEngineWrapper engine = new SimEngineWrapper();
            engine.selfTest();

        } catch (SimEngineLoadException e) {
            System.err.println("FATAL: Could not load SimEngine native library.");
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }
}