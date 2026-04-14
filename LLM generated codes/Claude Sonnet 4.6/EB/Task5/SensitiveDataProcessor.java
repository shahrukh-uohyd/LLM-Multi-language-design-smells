/**
 * Manages sensitive data processing.
 * Delegates encryption/processing to a native library.
 * Contains a private diagnostic logger that the native layer
 * can invoke through JNI when a native error occurs.
 */
public class SensitiveDataProcessor {

    static {
        System.loadLibrary("NativeProcessor");
    }

    // ── Internal state ───────────────────────────────────────────────────
    private final String           processorId;
    private final DiagnosticLogger logger;
    private       boolean          processingActive;
    private       int              errorCount;

    public SensitiveDataProcessor(String processorId) {
        this.processorId      = processorId;
        this.logger           = new DiagnosticLogger(processorId);
        this.processingActive = false;
        this.errorCount       = 0;
    }

    /* ── Getters (read by C via JNI) ────────────────────────────────── */
    public String           getProcessorId()      { return processorId;      }
    public DiagnosticLogger getLogger()           { return logger;           }
    public boolean          isProcessingActive()  { return processingActive; }
    public int              getErrorCount()       { return errorCount;       }

    /* ── Setters (written by C via JNI) ─────────────────────────────── */
    public void setProcessingActive(boolean active) { this.processingActive = active; }
    public void setErrorCount(int count)            { this.errorCount       = count;  }

    /* ── PRIVATE diagnostic logger ──────────────────────────────────── */

    /**
     * Private method intentionally hidden from public API.
     * Called by the native layer via JNI when a native error occurs.
     *
     * @param errorCode  numeric native error code
     * @param message    human-readable description of the native error
     */
    @SuppressWarnings("unused")          // Invoked reflectively via JNI
    private void logNativeError(int errorCode, String message) {
        errorCount++;
        String formatted = String.format(
                "NATIVE ERROR [code=%d, total=%d]: %s",
                errorCode, errorCount, message);
        logger.writeLog("ERROR", formatted);
    }

    /* ── Native methods ─────────────────────────────────────────────── */

    /**
     * Processes the given sensitive data payload in native code.
     * If a native error occurs the native layer calls logNativeError()
     * on this object before returning.
     *
     * @param data  payload to process
     * @return      processed result, or null on failure
     */
    public native byte[] processData(byte[] data);

    /**
     * Validates the integrity of a data block natively.
     *
     * @param data      block to validate
     * @param checksum  expected checksum value
     * @return          true if the block is valid
     */
    public native boolean validateData(byte[] data, long checksum);

    /**
     * Returns a full status report built in native code.
     */
    public native String getNativeStatusReport();

    /* ── Demo main ───────────────────────────────────��──────────────── */
    public static void main(String[] args) {

        SensitiveDataProcessor proc =
                new SensitiveDataProcessor("PROC-SECURE-001");

        System.out.println("=== Test 1: valid data processing ===");
        byte[] result = proc.processData("HelloNative".getBytes());
        System.out.println("Result : " + (result != null
                           ? new String(result) : "<null>"));
        System.out.println("Errors : " + proc.getErrorCount());

        System.out.println("\n=== Test 2: null payload (triggers native error) ===");
        result = proc.processData(null);
        System.out.println("Result : " + result);
        System.out.println("Errors : " + proc.getErrorCount());

        System.out.println("\n=== Test 3: checksum validation ===");
        byte[] block = "SecureBlock".getBytes();
        System.out.println("Valid   checksum : "
                           + proc.validateData(block, 12345L));
        System.out.println("Invalid checksum : "
                           + proc.validateData(block, 99999L));
        System.out.println("Errors : " + proc.getErrorCount());

        System.out.println("\n=== Status report ===");
        System.out.println(proc.getNativeStatusReport());
    }
}