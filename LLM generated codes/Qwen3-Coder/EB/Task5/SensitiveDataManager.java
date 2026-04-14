import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;

public class SensitiveDataManager {
    private String dataId;
    private byte[] sensitiveData;
    private volatile boolean isProcessing;
    private final ReentrantLock processingLock = new ReentrantLock();
    private String secureLogPath;
    private volatile int errorCount;

    // Load the native library
    static {
        System.loadLibrary("data_processor");
    }

    public SensitiveDataManager(String dataId, byte[] data, String logPath) {
        this.dataId = dataId;
        this.sensitiveData = data != null ? data.clone() : new byte[0];
        this.isProcessing = false;
        this.secureLogPath = logPath;
        this.errorCount = 0;
    }

    // Native methods
    public native boolean processSensitiveData(byte[] inputData);
    public native boolean triggerNativeError();
    public native void logDiagnosticMessage(String message, String level);

    /**
     * Private method for logging internal diagnostic messages to secure file
     */
    private void logInternalDiagnostic(String message, String level) {
        processingLock.lock();
        try {
            // Format the log entry
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String logEntry = String.format("[%s] [%s] [%s] %s%n", timestamp, level.toUpperCase(), dataId, message);
            
            // Write to secure log file
            try (FileWriter writer = new FileWriter(secureLogPath, true)) {
                writer.write(logEntry);
                writer.flush();
                
                // Sync to disk for security
                if (writer instanceof BufferedWriter) {
                    ((BufferedWriter) writer).getWriteBehindWriter().flush();
                }
            } catch (IOException e) {
                System.err.println("Failed to write to secure log: " + e.getMessage());
                // Fallback: write to system error stream
                System.err.printf("[%s] [ERROR] [LOG_WRITE_FAILED] Failed to log: %s%n", 
                                timestamp, message);
            }
            
            // Increment error count if this is an error log
            if ("ERROR".equalsIgnoreCase(level)) {
                errorCount++;
            }
            
        } finally {
            processingLock.unlock();
        }
    }

    /**
     * Public wrapper for testing the native logging functionality
     */
    public void testNativeLogging() {
        logInternalDiagnostic("Testing native logging functionality", "INFO");
        logInternalDiagnostic("This is a warning message from Java", "WARNING");
        logInternalDiagnostic("Simulated error from Java", "ERROR");
    }

    // Public methods for data management
    public boolean startProcessing() {
        processingLock.lock();
        try {
            if (!isProcessing) {
                isProcessing = true;
                logInternalDiagnostic("Starting data processing", "INFO");
                return true;
            }
            return false;
        } finally {
            processingLock.unlock();
        }
    }

    public boolean stopProcessing() {
        processingLock.lock();
        try {
            if (isProcessing) {
                isProcessing = false;
                logInternalDiagnostic("Stopping data processing", "INFO");
                return true;
            }
            return false;
        } finally {
            processingLock.unlock();
        }
    }

    public boolean isProcessing() {
        return isProcessing;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public String getDataId() {
        return dataId;
    }

    public int getDataSize() {
        return sensitiveData.length;
    }

    @Override
    public String toString() {
        return String.format("SensitiveDataManager{id='%s', size=%d, processing=%s, errors=%d}",
                dataId, sensitiveData.length, isProcessing, errorCount);
    }

    // Example usage
    public static void main(String[] args) {
        try {
            SensitiveDataManager manager = new SensitiveDataManager(
                "DATA-001", 
                "Sensitive data content".getBytes(), 
                "secure_diag.log"
            );
            
            System.out.println("Created manager: " + manager);
            
            // Test the Java-side logging
            manager.testNativeLogging();
            
            // Test native processing
            boolean processed = manager.processSensitiveData("Test data".getBytes());
            System.out.println("Native processing result: " + processed);
            
            // Trigger native error to test error logging
            boolean errorTriggered = manager.triggerNativeError();
            System.out.println("Native error triggered: " + errorTriggered);
            
            System.out.println("Final state: " + manager);
            
        } catch (Exception e) {
            System.err.println("Error in main: " + e.getMessage());
            e.printStackTrace();
        }
    }
}