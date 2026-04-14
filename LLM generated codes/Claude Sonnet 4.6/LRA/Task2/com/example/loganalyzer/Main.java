package com.example.loganalyzer;

import java.time.Instant;

/**
 * Demonstrates end-to-end usage of LogAnalyzer with a synthetic batch.
 */
public class Main {
    public static void main(String[] args) {
        LogEvent[] batch = {
            new LogEvent(Instant.now().toEpochMilli(), LogEvent.SEVERITY_INFO,    "AuthService",  "User login successful"),
            new LogEvent(Instant.now().toEpochMilli(), LogEvent.SEVERITY_DEBUG,   "AuthService",  "Token validated"),
            new LogEvent(Instant.now().toEpochMilli(), LogEvent.SEVERITY_WARNING, "DatabasePool", "Connection pool at 80% capacity"),
            new LogEvent(Instant.now().toEpochMilli(), LogEvent.SEVERITY_ERROR,   "DatabasePool", "Query timeout after 30s"),
            new LogEvent(Instant.now().toEpochMilli(), LogEvent.SEVERITY_INFO,    "ApiGateway",   "Request routed to /api/v2/users"),
            new LogEvent(Instant.now().toEpochMilli(), LogEvent.SEVERITY_FATAL,   "DatabasePool", "Connection pool exhausted — service unavailable"),
            new LogEvent(Instant.now().toEpochMilli(), LogEvent.SEVERITY_INFO,    "AuthService",  "Session refreshed"),
            new LogEvent(Instant.now().toEpochMilli(), LogEvent.SEVERITY_ERROR,   "ApiGateway",   "Upstream service returned 503"),
        };

        LogAnalyzer analyzer = new LogAnalyzer();
        LogSummary  summary  = analyzer.analyzeBatch(batch);

        System.out.println(summary);
        /*
         * Expected output (timestamps will vary):
         *
         * LogSummary{
         *   totalEvents=8
         *   DEBUG=1  INFO=3  WARNING=1  ERROR=2  FATAL=1
         *   firstTimestamp=...  lastTimestamp=...
         *   mostActiveComponent='DatabasePool' (3 events)
         * }
         */
    }
}