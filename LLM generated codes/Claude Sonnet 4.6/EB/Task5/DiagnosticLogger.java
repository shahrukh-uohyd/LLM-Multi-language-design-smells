import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;

/**
 * Writes diagnostic messages to a secure local log file.
 * Used internally by SensitiveDataProcessor — not exposed publicly.
 */
public class DiagnosticLogger {

    private static final String LOG_FILE = "secure_diagnostic.log";

    private final String  componentName;   // e.g. "SensitiveDataProcessor"
    private       boolean enabled;

    public DiagnosticLogger(String componentName) {
        this.componentName = componentName;
        this.enabled       = true;
    }

    /* ── Getters / setters (read by C via JNI) ──────────────────────── */
    public String  getComponentName() { return componentName; }
    public boolean isEnabled()        { return enabled;       }
    public void    setEnabled(boolean enabled) { this.enabled = enabled; }

    /**
     * Appends a timestamped entry to the secure log file.
     *
     * @param level    severity level: "INFO", "WARN", "ERROR", "FATAL"
     * @param message  diagnostic message
     */
    public void writeLog(String level, String message) {
        if (!enabled) return;
        String entry = String.format("[%s] [%s] [%s] %s%n",
                Instant.now(), level, componentName, message);
        try (PrintWriter pw =
                new PrintWriter(new FileWriter(LOG_FILE, /*append=*/true))) {
            pw.print(entry);
        } catch (IOException e) {
            System.err.println("DiagnosticLogger: failed to write log — "
                               + e.getMessage());
        }
        System.out.print("  [LOG] " + entry);   // Echo to stdout for demo
    }
}