package com.example.logs;

import java.util.EnumMap;
import java.util.Map;

public class LogAnalysisApp {

    public static void main(String[] args) {

        LogEvent[] batch = {
            new LogEvent(System.currentTimeMillis(),
                    Severity.INFO, "Auth", "User logged in"),
            new LogEvent(System.currentTimeMillis(),
                    Severity.ERROR, "DB", "Connection failed"),
            new LogEvent(System.currentTimeMillis(),
                    Severity.INFO, "Auth", "Token refreshed"),
            new LogEvent(System.currentTimeMillis(),
                    Severity.WARN, "Cache", "Cache miss")
        };

        int[] counts = NativeLogAnalyzer.countBySeverity(
                batch, Severity.values().length);

        Map<Severity, Integer> summary =
                new EnumMap<>(Severity.class);

        for (Severity s : Severity.values()) {
            summary.put(s, counts[s.ordinal()]);
        }

        System.out.println(summary);
    }
}
