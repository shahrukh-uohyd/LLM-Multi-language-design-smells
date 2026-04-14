/**
 * Java wrapper for the 'sensor-io-x64' manufacturer-supplied native driver.
 *
 * Responsibilities:
 *   1. Loads the 'sensor-io-x64' shared library once when the class is
 *      first accessed (static initialiser).
 *   2. Declares the native method readRawData() which returns an int[].
 *   3. Provides a safe, validated public API on top of the raw native call.
 *   4. Exposes library-load status so callers can react without catching
 *      unexpected runtime exceptions.
 *
 * Native library file names resolved by the JVM per platform:
 *   Linux   →  libsensor-io-x64.so
 *   macOS   →  libsensor-io-x64.dylib
 *   Windows →  sensor-io-x64.dll
 *
 * Place the library on java.library.path at runtime, e.g.:
 *   java -Djava.library.path=/opt/sensor-io/lib -cp out Main
 */
public class SensorIO {

    // ------------------------------------------------------------------ //
    //  Library name constant                                               //
    // ------------------------------------------------------------------ //

    /** Bare library name passed to {@link System#loadLibrary(String)}. */
    private static final String LIBRARY_NAME = "sensor-io-x64";

    // ------------------------------------------------------------------ //
    //  Library load state (set once in the static initialiser)            //
    // ------------------------------------------------------------------ //

    /** {@code true} only when the native library loaded without error. */
    private static final boolean LIBRARY_LOADED;

    /**
     * Human-readable reason for a failed load attempt.
     * {@code null} when loading succeeded.
     */
    private static final String LOAD_ERROR;

    // ------------------------------------------------------------------ //
    //  Static initialiser – executes once on first class access           //
    // ------------------------------------------------------------------ //

    static {
        boolean loaded = false;
        String  error  = null;

        try {
            /*
             * System.loadLibrary() locates the shared object / DLL by
             * searching every directory listed in java.library.path.
             * The JVM prepends "lib" and appends the platform extension
             * automatically, so only the bare name is needed here.
             */
            System.loadLibrary(LIBRARY_NAME);
            loaded = true;
            System.out.println(
                "[SensorIO] Native library '" + LIBRARY_NAME + "' loaded successfully.");

        } catch (UnsatisfiedLinkError ule) {
            /*
             * Thrown when:
             *   • The library file cannot be found on java.library.path.
             *   • The file exists but is the wrong architecture (e.g. 32-bit
             *     library on a 64-bit JVM).
             *   • A symbol referenced by the library is missing at link time.
             */
            error = "UnsatisfiedLinkError – library '" + LIBRARY_NAME + "' not found "
                  + "or incompatible architecture. "
                  + "Verify java.library.path and that a 64-bit build is present. "
                  + "Detail: " + ule.getMessage();
            System.err.println("[SensorIO] " + error);

        } catch (SecurityException se) {
            /*
             * Thrown when the active SecurityManager forbids native-library
             * loading (relevant in sandboxed / restricted environments).
             */
            error = "SecurityException – permission denied while loading '"
                  + LIBRARY_NAME + "'. Detail: " + se.getMessage();
            System.err.println("[SensorIO] " + error);
        }

        LIBRARY_LOADED = loaded;
        LOAD_ERROR     = error;
    }

    // ------------------------------------------------------------------ //
    //  Native method declaration                                           //
    // ------------------------------------------------------------------ //

    /**
     * Reads a burst of raw sensor data directly from the hardware driver.
     *
     * <p>The returned array contains the driver's native integer readings.
     * The array length, unit, and encoding are defined by the manufacturer's
     * driver specification for the sensor-io-x64 device.</p>
     *
     * <p>Common interpretations (driver-version dependent):
     * <ul>
     *   <li>Each element may represent one ADC channel reading.</li>
     *   <li>Values may be raw counts requiring scaling via a calibration factor.</li>
     *   <li>A {@code null} return signals a hardware-level read failure.</li>
     * </ul>
     * </p>
     *
     * @return  array of raw integer sensor readings, or {@code null} on
     *          hardware / driver error
     */
    public native int[] readRawData();

    // ------------------------------------------------------------------ //
    //  Constructor                                                         //
    // ------------------------------------------------------------------ //

    /**
     * Creates a new {@code SensorIO} instance.
     *
     * @throws IllegalStateException if the native library failed to load
     */
    public SensorIO() {
        requireLibrary();
    }

    // ------------------------------------------------------------------ //
    //  Public safe API                                                     //
    // ------------------------------------------------------------------ //

    /**
     * Reads raw sensor data from the hardware and returns it as a
     * {@link SensorReading} value object.
     *
     * <p>This method validates the native result before returning it,
     * throwing a typed exception instead of passing {@code null} or
     * an empty array to the caller.</p>
     *
     * @return  a {@link SensorReading} wrapping the validated raw data
     * @throws IllegalStateException  if the native library is not loaded
     * @throws SensorIOException      if the driver returns null or empty data
     */
    public SensorReading read() {
        requireLibrary();

        int[] raw = readRawData();

        if (raw == null)
            throw new SensorIOException(
                "readRawData() returned null – the driver reported a " +
                "hardware read failure.");

        if (raw.length == 0)
            throw new SensorIOException(
                "readRawData() returned an empty array – " +
                "no sensor channels were read.");

        return new SensorReading(raw, System.currentTimeMillis());
    }

    /**
     * Attempts to read raw sensor data, returning {@code null} instead of
     * throwing when the driver reports a failure.
     *
     * <p>Useful in polling loops where a single missed reading should not
     * abort the monitoring session.</p>
     *
     * @return  a {@link SensorReading}, or {@code null} on driver failure
     * @throws IllegalStateException if the native library is not loaded
     */
    public SensorReading tryRead() {
        requireLibrary();
        try {
            return read();
        } catch (SensorIOException e) {
            System.err.println("[SensorIO] tryRead() suppressed error: "
                               + e.getMessage());
            return null;
        }
    }

    /**
     * Performs {@code count} successive reads, each separated by
     * {@code intervalMs} milliseconds, and returns all readings.
     *
     * <p>Failed individual reads are recorded as {@code null} entries in the
     * returned array so that the caller can detect gaps without losing the
     * timing information of surrounding successful reads.</p>
     *
     * @param count       number of reads to perform (must be &gt; 0)
     * @param intervalMs  pause between reads in milliseconds (must be &ge; 0)
     * @return            array of {@link SensorReading} objects (may contain nulls)
     * @throws IllegalArgumentException if count &le; 0 or intervalMs &lt; 0
     * @throws IllegalStateException    if the native library is not loaded
     */
    public SensorReading[] readBurst(int count, long intervalMs) {
        requireLibrary();

        if (count <= 0)
            throw new IllegalArgumentException(
                "count must be greater than 0, got: " + count);
        if (intervalMs < 0)
            throw new IllegalArgumentException(
                "intervalMs must be >= 0, got: " + intervalMs);

        SensorReading[] results = new SensorReading[count];

        for (int i = 0; i < count; i++) {
            results[i] = tryRead();

            if (intervalMs > 0 && i < count - 1) {
                try {
                    Thread.sleep(intervalMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();   // restore flag
                    System.err.println(
                        "[SensorIO] readBurst() interrupted at sample " + i);
                    break;
                }
            }
        }

        return results;
    }

    // ------------------------------------------------------------------ //
    //  Library-state inspection                                            //
    // ------------------------------------------------------------------ //

    /**
     * Returns {@code true} if the native library loaded successfully.
     *
     * @return  library load status
     */
    public static boolean isLibraryLoaded() {
        return LIBRARY_LOADED;
    }

    /**
     * Returns the error message produced during a failed library load,
     * or {@code null} if loading succeeded.
     *
     * @return  load-error string, or {@code null}
     */
    public static String getLoadError() {
        return LOAD_ERROR;
    }

    /**
     * Returns the bare name of the native library this class binds to.
     *
     * @return  library name string
     */
    public static String getLibraryName() {
        return LIBRARY_NAME;
    }

    // ------------------------------------------------------------------ //
    //  Private guard                                                       //
    // ------------------------------------------------------------------ //

    /**
     * Throws {@link IllegalStateException} if the native library is not
     * loaded. Called at the entry point of every method that requires the
     * library to be present.
     */
    private static void requireLibrary() {
        if (!LIBRARY_LOADED)
            throw new IllegalStateException(
                "Native library '" + LIBRARY_NAME + "' is not loaded. " +
                "Cause: " + LOAD_ERROR);
    }

    // ================================================================== //
    //  Value objects                                                       //
    // ================================================================== //

    /**
     * Immutable snapshot of one raw-data read from the sensor driver.
     *
     * <p>Carries the raw integer array together with a wall-clock timestamp
     * and derived statistics (min, max, sum, mean) computed in pure Java.</p>
     */
    public static class SensorReading {

        /** Raw integer values returned by the native driver. */
        public final int[] rawData;

        /** System wall-clock time (ms since epoch) at the moment of the read. */
        public final long  timestampMs;

        /** Number of data points in this reading. */
        public final int   channelCount;

        /** Minimum value across all channels. */
        public final int   min;

        /** Maximum value across all channels. */
        public final int   max;

        /** Sum of all channel values. */
        public final long  sum;

        /** Arithmetic mean of all channel values. */
        public final double mean;

        /**
         * Constructs a {@code SensorReading} from a validated raw array.
         *
         * @param rawData      non-null, non-empty int array from the driver
         * @param timestampMs  capture timestamp (ms since epoch)
         */
        public SensorReading(int[] rawData, long timestampMs) {
            // Defensive copy – caller must not mutate the stored data
            this.rawData      = java.util.Arrays.copyOf(rawData, rawData.length);
            this.timestampMs  = timestampMs;
            this.channelCount = rawData.length;

            // Single-pass statistics
            int  lo   = rawData[0];
            int  hi   = rawData[0];
            long total = 0L;
            for (int v : rawData) {
                if (v < lo) lo = v;
                if (v > hi) hi = v;
                total += v;
            }
            this.min  = lo;
            this.max  = hi;
            this.sum  = total;
            this.mean = (double) total / rawData.length;
        }

        /**
         * Returns the raw value for a specific channel index.
         *
         * @param channel  zero-based channel index
         * @return         raw integer value
         * @throws IndexOutOfBoundsException if channel is out of range
         */
        public int getChannel(int channel) {
            if (channel < 0 || channel >= channelCount)
                throw new IndexOutOfBoundsException(
                    "Channel " + channel + " out of range [0, " +
                    (channelCount - 1) + "]");
            return rawData[channel];
        }

        /**
         * Returns a formatted timestamp string (ISO-like, local time).
         *
         * @return  timestamp as "yyyy-MM-dd HH:mm:ss.SSS"
         */
        public String getFormattedTimestamp() {
            java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            return sdf.format(new java.util.Date(timestampMs));
        }

        @Override
        public String toString() {
            return String.format(
                "SensorReading{timestamp='%s', channels=%d, " +
                "min=%d, max=%d, mean=%.2f, sum=%d, data=%s}",
                getFormattedTimestamp(),
                channelCount,
                min, max, mean, sum,
                java.util.Arrays.toString(rawData));
        }
    }

    // ================================================================== //
    //  Exception type                                                      //
    // ================================================================== //

    /**
     * Thrown when a native sensor-driver operation fails at runtime.
     *
     * <p>Extends {@link RuntimeException} so callers are not forced to
     * declare it, but it is specific enough to be caught and handled
     * separately from other unexpected exceptions.</p>
     */
    public static class SensorIOException extends RuntimeException {

        /**
         * Constructs a {@code SensorIOException} with a detail message.
         *
         * @param message  description of the failure
         */
        public SensorIOException(String message) {
            super(message);
        }

        /**
         * Constructs a {@code SensorIOException} with a detail message and
         * an underlying cause.
         *
         * @param message  description of the failure
         * @param cause    the underlying throwable
         */
        public SensorIOException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}