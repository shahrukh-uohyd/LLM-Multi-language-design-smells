#include <jni.h>
#include <stdio.h>
#include <string.h>

/*
 * Valid operation names and their JNI method signatures.
 * All Calculator methods share the same signature: (II)I
 *   - takes two ints (I, I)
 *   - returns an int (I)
 */
#define NUM_OPERATIONS 4

static const char *VALID_OPERATIONS[NUM_OPERATIONS] = {
    "add", "subtract", "multiply", "divide"
};

/*
 * Implements: public native int invokeOperation(Calculator calc,
 *                                               String operation,
 *                                               int a, int b);
 *
 * JNI function name (no package prefix since default package is used):
 *   Java_NativeBridge_invokeOperation
 *
 * Dynamic dispatch logic:
 *   1. Convert the Java String "operation" to a C string.
 *   2. Validate the operation name against the known list.
 *   3. Get the Class of the Calculator object.
 *   4. Look up the method ID dynamically using the operation name.
 *   5. Invoke the method via CallIntMethod.
 *   6. Return the int result to Java.
 */
JNIEXPORT jint JNICALL
Java_NativeBridge_invokeOperation(JNIEnv *env,
                                  jobject  thisObj,
                                  jobject  calcObj,
                                  jstring  operationStr,
                                  jint     a,
                                  jint     b)
{
    /* ── Step 1: Convert the Java String to a C string ── */
    const char *opName = (*env)->GetStringUTFChars(env, operationStr, NULL);
    if (opName == NULL) {
        fprintf(stderr, "[native] ERROR: Failed to read operation string.\n");
        return -1;
    }

    printf("[native] Requested operation : \"%s\"  with a=%d, b=%d\n", opName, a, b);

    /* ── Step 2: Validate operation name against the known list ── */
    int isValid = 0;
    for (int i = 0; i < NUM_OPERATIONS; i++) {
        if (strcmp(opName, VALID_OPERATIONS[i]) == 0) {
            isValid = 1;
            break;
        }
    }

    if (!isValid) {
        fprintf(stderr,
            "[native] ERROR: Unknown operation \"%s\". "
            "Valid options: add, subtract, multiply, divide.\n",
            opName);
        (*env)->ReleaseStringUTFChars(env, operationStr, opName);
        return -1;
    }

    /* ── Step 3: Get the Class of the Calculator object ── */
    jclass calcClass = (*env)->GetObjectClass(env, calcObj);
    if (calcClass == NULL) {
        fprintf(stderr, "[native] ERROR: Could not get Calculator class.\n");
        (*env)->ReleaseStringUTFChars(env, operationStr, opName);
        return -1;
    }

    /*
     * ── Step 4: Look up the method ID dynamically ──
     *
     * GetMethodID(env, class, methodName, methodSignature)
     *
     * All four Calculator methods have the same JNI signature:
     *   (II)I  →  takes (int, int), returns int
     *
     * The method name (opName) is resolved at RUNTIME — this is
     * what makes the invocation truly dynamic.
     */
    jmethodID methodID = (*env)->GetMethodID(env, calcClass, opName, "(II)I");
    if (methodID == NULL) {
        fprintf(stderr,
            "[native] ERROR: Could not find method \"%s\" with signature (II)I.\n",
            opName);
        (*env)->ExceptionClear(env);
        (*env)->ReleaseStringUTFChars(env, operationStr, opName);
        return -1;
    }

    printf("[native] Method ID resolved for \"%s\" — invoking now...\n", opName);

    /* ── Step 5: Dynamically invoke the method on the Calculator object ── */
    jint result = (*env)->CallIntMethod(env, calcObj, methodID, a, b);

    /* Check if the invoked method threw an exception */
    if ((*env)->ExceptionCheck(env)) {
        fprintf(stderr, "[native] ERROR: Exception thrown during \"%s\" invocation.\n", opName);
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        (*env)->ReleaseStringUTFChars(env, operationStr, opName);
        return -1;
    }

    printf("[native] \"%s\"(%d, %d) returned: %d\n", opName, a, b, result);

    /* ── Step 6: Release C string and return result to Java ── */
    (*env)->ReleaseStringUTFChars(env, operationStr, opName);
    return result;
}