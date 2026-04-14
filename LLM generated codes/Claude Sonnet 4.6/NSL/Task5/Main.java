/**
 * Demonstrates the SensorIO wrapper in a typical industrial-monitoring loop.
 *
 * Run:
 *   java -Djava.library.path=/path/to/sensor-io-x64/lib -cp out Main
 */
public class Main {

    public static void main(String[] args) {

        // ---------------------------------------------------------------- //
        // 1. Check library load state before creating any instance          //
        // ---------------------------------------------------------------- //
        System.out.println("Library      : " + SensorIO.getLibraryName());
        System.out.println("Load success : " + SensorIO.isLibraryLoaded());

        if (!SensorIO.isLibraryLoaded()) {
            System.err.println("Cannot continue – library not available.");
            System.err.println("Reason: " + SensorIO.getLoadError());
            System.exit(1);
        }

        SensorIO sensor = new SensorIO();

        // ---------------------------------------------------------------- //
        // 2. Single validated read via read()                               //
        // ---------------------------------------------------------------- //
        System.out.println("\n--- Single Read ---");
        try {
            SensorIO.SensorReading reading = sensor.read();
            System.out.println(reading);
            System.out.println("Channel 0 value : " + reading.getChannel(0));
        } catch (SensorIO.SensorIOException e) {
            System.err.println("Read failed: " + e.getMessage());
        }

        // ---------------------------------------------------------------- //
        // 3. Fault-tolerant read via tryRead()                              //
        // ---------------------------------------------------------------- //
        System.out.println("\n--- Fault-tolerant Read ---");
        SensorIO.SensorReading safe = sensor.tryRead();
        if (safe != null) {
            System.out.printf("  mean=%.2f  min=%d  max=%d%n",
                              safe.mean, safe.min, safe.max);
        } else {
            System.out.println("  Driver returned no data (hardware fault?).");
        }

        // ---------------------------------------------------------------- //
        // 4. Burst read – 5 samples, 200 ms apart                          //
        // ---------------------------------------------------------------- //
        System.out.println("\n--- Burst Read (5 samples @ 200 ms) ---");
        SensorIO.SensorReading[] burst = sensor.readBurst(5, 200L);
        int failures = 0;
        for (int i = 0; i < burst.length; i++) {
            if (burst[i] != null) {
                System.out.printf("  [%d] %s%n", i, burst[i]);
            } else {
                System.out.printf("  [%d] FAILED%n", i);
                failures++;
            }
        }
        System.out.printf("  Burst complete: %d/%d successful.%n",
                          burst.length - failures, burst.length);

        // ---------------------------------------------------------------- //
        // 5. Direct native call (no wrapping)                               //
        // ---------------------------------------------------------------- //
        System.out.println("\n--- Direct Native Call ---");
        int[] raw = sensor.readRawData();
        if (raw != null && raw.length > 0) {
            System.out.print("  Raw int[]: ");
            for (int v : raw) System.out.print(v + " ");
            System.out.println();
        } else {
            System.out.println("  Native readRawData() returned null or empty.");
        }
    }
}