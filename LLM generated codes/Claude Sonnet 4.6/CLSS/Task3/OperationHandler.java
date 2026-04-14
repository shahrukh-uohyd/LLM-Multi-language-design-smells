package com.example.operations;

/**
 * Java class whose methods are triggered by native C++ code via JNI.
 * Contains both instance-level and static operation entry points.
 */
public class OperationHandler {

    private final String handlerName;

    public OperationHandler(String handlerName) {
        this.handlerName = handlerName;
    }

    /**
     * Instance method invoked by native code.
     * Performs an operation identified by operationType with a given priority.
     *
     * @param operationType  A string tag identifying the operation to perform.
     * @param priority       An integer priority level for the operation.
     */
    public void performOperation(String operationType, int priority) {
        System.out.printf("[%s] Performing operation: type='%s', priority=%d%n",
                handlerName, operationType, priority);

        // Dispatch to specific logic based on operation type
        switch (operationType) {
            case "SYNC"     -> handleSync(priority);
            case "FLUSH"    -> handleFlush(priority);
            case "SHUTDOWN" -> handleShutdown(priority);
            default         -> System.err.println("Unknown operation type: " + operationType);
        }
    }

    /**
     * Static method invoked by native code — no instance required.
     *
     * @param eventCode  A numeric event code originating from native code.
     * @param payload    A descriptive payload string.
     */
    public static void handleNativeEvent(int eventCode, String payload) {
        System.out.printf("[Static] Native event received: code=%d, payload='%s'%n",
                eventCode, payload);
    }

    // --- Private operation implementations ---

    private void handleSync(int priority) {
        System.out.println("  → Executing SYNC at priority " + priority);
    }

    private void handleFlush(int priority) {
        System.out.println("  → Executing FLUSH at priority " + priority);
    }

    private void handleShutdown(int priority) {
        System.out.println("  → Executing SHUTDOWN at priority " + priority);
    }
}