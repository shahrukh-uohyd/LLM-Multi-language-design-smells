// NativeDispatcher.c
#include <jni.h>
#include <string.h>
#include "NativeDispatcher.h"

JNIEXPORT jint JNICALL
Java_NativeDispatcher_invokeOperation(
        JNIEnv *env,
        jobject thisObj,
        jobject calculatorObj,
        jstring operation,
        jint a,
        jint b) {

    // Convert Java String to C string
    const char *op = (*env)->GetStringUTFChars(env, operation, NULL);

    // Get Calculator class
    jclass calculatorClass = (*env)->GetObjectClass(env, calculatorObj);

    jmethodID methodId = NULL;

    // Decide which method to invoke at runtime
    if (strcmp(op, "add") == 0) {
        methodId = (*env)->GetMethodID(env, calculatorClass, "add", "(II)I");
    } else if (strcmp(op, "multiply") == 0) {
        methodId = (*env)->GetMethodID(env, calculatorClass, "multiply", "(II)I");
    } else if (strcmp(op, "subtract") == 0) {
        methodId = (*env)->GetMethodID(env, calculatorClass, "subtract", "(II)I");
    }

    // Release UTF string
    (*env)->ReleaseStringUTFChars(env, operation, op);

    if (methodId == NULL) {
        // Unknown operation → signal error
        return -1;
    }

    // Invoke the selected Java method
    jint result = (*env)->CallIntMethod(
            env,
            calculatorObj,
            methodId,
            a,
            b
    );

    return result;
}
