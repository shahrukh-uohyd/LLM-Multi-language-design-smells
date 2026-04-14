#include "NativeCalculationBridge.h"

#include <jni.h>
#include <string>
#include <thread>
#include <chrono>
#include <stdexcept>

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

/**
 * Holds the resolved JNI references needed to invoke the Java callback.
 * Resolved once at the start of startCalculation() to avoid repeated lookups.
 */
struct CallbackContext {
    JNIEnv*  env;          // Valid only on the thread that called startCalculation()
    jobject  handler;      // Local ref to the CalculationStatusHandler object
    jmethodID onStatusUpdate; // Resolved method ID for onStatusUpdate(int, String)
};

/**
 * Sends a status notification back to the Java handler.
 *
 * @param ctx         The resolved callback context.
 * @param statusCode  Integer status/progress code.
 * @param message     Human-readable description string.
 */
static void notifyJavaHandler(const CallbackContext& ctx,
                               jint statusCode,
                               const std::string& message)
{
    // Convert std::string → jstring
    jstring jMessage = ctx.env->NewStringUTF(message.c_str());
    if (jMessage == nullptr) {
        // NewStringUTF throws OutOfMemoryError; bail out
        return;
    }

    // Invoke handler.onStatusUpdate(statusCode, message)
    ctx.env->CallVoidMethod(ctx.handler, ctx.onStatusUpdate, statusCode, jMessage);

    // Release the local jstring reference immediately after use
    ctx.env->DeleteLocalRef(jMessage);

    // Check if the Java call threw an exception and clear it so execution continues.
    // You can choose to propagate it by NOT calling ExceptionClear().
    if (ctx.env->ExceptionCheck()) {
        ctx.env->ExceptionDescribe(); // Prints stack trace to stderr
        ctx.env->ExceptionClear();
    }
}

/**
 * The actual long-running calculation logic.
 * Replace the body with your real computation.
 *
 * Calls notifyJavaHandler() periodically to report progress.
 *
 * @param ctx  Resolved JNI callback context.
 * @return     0 on success, non-zero on failure.
 */
static jint performCalculation(const CallbackContext& ctx)
{
    const int TOTAL_STEPS = 10;

    for (int step = 1; step <= TOTAL_STEPS; ++step) {

        // --- Simulate work (replace with real computation) ---
        std::this_thread::sleep_for(std::chrono::milliseconds(500));

        // --- Build a descriptive status message ---
        int progressPercent = (step * 100) / TOTAL_STEPS;
        std::string message = "Step " + std::to_string(step)
                            + " of " + std::to_string(TOTAL_STEPS)
                            + " complete (" + std::to_string(progressPercent) + "%)";

        // --- Notify Java ---
        notifyJavaHandler(ctx, progressPercent, message);

        // --- Abort early if a Java exception is pending ---
        if (ctx.env->ExceptionCheck()) {
            return -1;
        }
    }

    // Final "done" notification
    notifyJavaHandler(ctx, 100, "Calculation completed successfully.");
    return 0;
}

// ---------------------------------------------------------------------------
// JNI Entry Point
// ---------------------------------------------------------------------------

JNIEXPORT jint JNICALL
Java_com_example_jni_NativeCalculationBridge_startCalculation(
    JNIEnv* env,
    jobject /* thiz */,
    jobject handler)
{
    // -----------------------------------------------------------------------
    // 1. Validate the handler object
    // -----------------------------------------------------------------------
    if (handler == nullptr) {
        env->ThrowNew(
            env->FindClass("java/lang/IllegalArgumentException"),
            "handler must not be null"
        );
        return -1;
    }

    // -----------------------------------------------------------------------
    // 2. Resolve the handler's class and method ID
    //    Method signature: void onStatusUpdate(int, String)
    //    JNI descriptor:   (ILjava/lang/String;)V
    // -----------------------------------------------------------------------
    jclass handlerClass = env->GetObjectClass(handler);
    if (handlerClass == nullptr) {
        // Should never happen if handler != null, but guard anyway
        env->ThrowNew(
            env->FindClass("java/lang/RuntimeException"),
            "Failed to get class of handler object"
        );
        return -1;
    }

    jmethodID onStatusUpdateMethod = env->GetMethodID(
        handlerClass,
        "onStatusUpdate",           // Java method name
        "(ILjava/lang/String;)V"    // JNI descriptor: (int, String) → void
    );

    env->DeleteLocalRef(handlerClass); // Done with the class ref

    if (onStatusUpdateMethod == nullptr) {
        // GetMethodID throws NoSuchMethodError automatically; just return.
        return -1;
    }

    // -----------------------------------------------------------------------
    // 3. Build the callback context
    // -----------------------------------------------------------------------
    CallbackContext ctx {
        /* env            = */ env,
        /* handler        = */ handler,
        /* onStatusUpdate = */ onStatusUpdateMethod
    };

    // -----------------------------------------------------------------------
    // 4. Run the long calculation with periodic callbacks
    // -----------------------------------------------------------------------
    return performCalculation(ctx);
}