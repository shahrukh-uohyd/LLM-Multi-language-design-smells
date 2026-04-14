#include <jni.h>
#include "SensitiveProcessor.h"

JNIEXPORT void JNICALL
Java_SensitiveProcessor_processSensitiveData(JNIEnv *env, jobject obj) {

    // Simulate a native error condition
    int nativeError = 1;

    if (nativeError) {

        // Get the Java class of the calling object
        jclass cls = (*env)->GetObjectClass(env, obj);
        if (cls == NULL) {
            return;
        }

        // Locate the PRIVATE method:
        // private void logInternal(String message)
        jmethodID logMethod = (*env)->GetMethodID(
            env,
            cls,
            "logInternal",
            "(Ljava/lang/String;)V"
        );

        if (logMethod == NULL) {
            // Method not found (signature mismatch, etc.)
            (*env)->ExceptionClear(env);
            return;
        }

        // Create Java String message
        jstring msg = (*env)->NewStringUTF(
            env,
            "Native error occurred during sensitive processing"
        );

        // Invoke private Java logger
        (*env)->CallVoidMethod(env, obj, logMethod, msg);

        // Clean up local reference
        (*env)->DeleteLocalRef(env, msg);
    }
}
