package com.example.app;

/**
 * Manages app-level lifecycle transitions triggered by the native module.
 */
public class AppLifecycleManager {

    /**
     * Called by native code to signal a lifecycle event.
     *
     * @param event    Lifecycle event name (e.g. "PAUSE", "RESUME", "STOP").
     * @param reason   Human-readable reason string originating from native.
     */
    public static void onLifecycleEvent(String event, String reason) {
        System.out.printf("[Lifecycle] Event='%s', Reason='%s'%n", event, reason);

        switch (event) {
            case "PAUSE"  -> System.out.println("  → Application pausing.");
            case "RESUME" -> System.out.println("  → Application resuming.");
            case "STOP"   -> System.out.println("  → Application stopping.");
            default       -> System.err.println("  → Unknown lifecycle event: " + event);
        }
    }
}