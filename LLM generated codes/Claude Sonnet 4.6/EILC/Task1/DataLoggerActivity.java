package com.example.datalogger;

import com.example.datalogger.jni.SensorLogJniBridge;
import com.example.datalogger.model.SensorLog;
import java.io.File;
import java.util.List;

public class DataLoggerActivity {

    private static final String DB_NAME = "sensor_data.db";

    /**
     * Called after 30 sensor logs have been captured.
     */
    public void onLogsReady(List<SensorLog> capturedLogs) {
        // capturedLogs.size() == 30

        File dbFile = new File(getFilesDir(), DB_NAME); // Android internal storage
        String dbPath = dbFile.getAbsolutePath();

        try (SensorLogJniBridge bridge = new SensorLogJniBridge(dbPath)) {

            int saved = bridge.syncLogs(capturedLogs);

            if (saved == capturedLogs.size()) {
                System.out.println("All " + saved + " logs saved to native DB.");
            } else {
                System.err.println("Sync failed — 0 logs persisted (transaction rolled back).");
            }

        } catch (Exception e) {
            System.err.println("Error syncing logs: " + e.getMessage());
        }
    }

    // Stub — replace with real Android context call
    private File getFilesDir() {
        return new File("/data/user/0/com.example.datalogger/files");
    }
}