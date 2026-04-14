#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* ══════════════════════════════════════════════════════════════════════════
 *  Error code definitions
 * ══════════════════════════════════════════════════════════════════════════ */
#define ERR_NULL_INPUT        1001
#define ERR_PROCESSING_FAILED 1002
#define ERR_CHECKSUM_MISMATCH 1003
#define ERR_INVALID_STATE     1004

/* ══════════════════════════════════════════════════════════════════════════
 *  Utility: throw a Java RuntimeException from C
 * ══════════════════════════════════════════════════════════════════════════ */
static void throwJavaException(JNIEnv *env, const char *msg)
{
    jclass cls = (*env)->FindClass(env, "java/lang/RuntimeException");
    if (cls) (*env)->ThrowNew(env, cls, msg);
}

/* ══════════════════════════════════════════════════════════════════════════
 *  Utility: jstring <-> C string helpers
 * ══════════════════════════════════════════════════════════════════════════ */
static const char *jstrToC(JNIEnv *env, jstring jstr)
{
    if (!jstr) return NULL;
    return (*env)->GetStringUTFChars(env, jstr, NULL);
}

static void releaseJstr(JNIEnv *env, jstring jstr, const char *cstr)
{
    if (jstr && cstr)
        (*env)->ReleaseStringUTFChars(env, jstr, cstr);
}

static jstring cToJstr(JNIEnv *env, const char *cstr)
{
    return (*env)->NewStringUTF(env, cstr ? cstr : "");
}

/* ══════════════════════════════════════════════════════════════════════════
 *  Core: Invoke the PRIVATE logNativeError(int, String) method
 *
 *  Strategy:
 *    - Private methods are NOT accessible via GetMethodID with normal lookup
 *      because they cannot be overridden; we must use Class.getDeclaredMethod()
 *      (Java reflection) and then call setAccessible(true) before invoking it.
 *
 *  Call chain:
 *    Class   procClass   = processorObj.getClass();
 *    Method  logMethod   = procClass.getDeclaredMethod(
 *                              "logNativeError",
 *                              new Class[]{ int.class, String.class });
 *    logMethod.setAccessible(true);
 *    logMethod.invoke(processorObj, errorCode, message);
 * ══════════════════════════════════════════════════════════════════════════ */
static void invokePrivateLogger(JNIEnv  *env,
                                jobject  processorObj,
                                jint     errorCode,
                                const char *message)
{
    /* ── 1. Get Class object of the processor ─────────────────────────── */
    jclass    objectClass   = (*env)->FindClass(env, "java/lang/Object");
    jmethodID getClassMID   = (*env)->GetMethodID(
                                  env, objectClass,
                                  "getClass", "()Ljava/lang/Class;");
    jobject   procClassObj  = (*env)->CallObjectMethod(
                                  env, processorObj, getClassMID);
    if (!procClassObj) {
        fprintf(stderr, "[NATIVE] invokePrivateLogger: getClass() failed\n");
        return;
    }

    /* ── 2. Build the parameter type array: { int.class, String.class } ── */
    /* Resolve primitive int.class via Integer.TYPE                         */
    jclass integerClass = (*env)->FindClass(env, "java/lang/Integer");
    jfieldID typeField  = (*env)->GetStaticFieldID(
                              env, integerClass, "TYPE", "Ljava/lang/Class;");
    jobject  intClass   = (*env)->GetStaticObjectField(
                              env, integerClass, typeField);

    jclass stringClass  = (*env)->FindClass(env, "java/lang/String");

    /* Class[] paramTypes = new Class[2]; */
    jclass classOfClass  = (*env)->FindClass(env, "java/lang/Class");
    jobjectArray paramTypes =
        (*env)->NewObjectArray(env, 2, classOfClass, NULL);
    (*env)->SetObjectArrayElement(env, paramTypes, 0, intClass);
    (*env)->SetObjectArrayElement(env, paramTypes, 1,
                                  (jobject)stringClass);

    /* ── 3. Call Class.getDeclaredMethod(name, paramTypes) ──────────────
     *   This bypasses the normal method-resolution rules and returns the
     *   method regardless of its access modifier.                          */
    jclass classClass       = (*env)->GetObjectClass(env, procClassObj);
    jmethodID getDeclMID    = (*env)->GetMethodID(
                                  env, classClass,
                                  "getDeclaredMethod",
                                  "(Ljava/lang/String;[Ljava/lang/Class;)"
                                  "Ljava/lang/reflect/Method;");
    if (!getDeclMID) {
        fprintf(stderr,
                "[NATIVE] invokePrivateLogger: getDeclaredMethod not found\n");
        return;
    }

    jstring methodName = cToJstr(env, "logNativeError");
    jobject methodObj  = (*env)->CallObjectMethod(
                             env, procClassObj,
                             getDeclMID, methodName, paramTypes);

    /* Clear any NoSuchMethodException before checking the result */
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        fprintf(stderr,
                "[NATIVE] invokePrivateLogger: "
                "logNativeError method not found\n");
        return;
    }
    if (!methodObj) {
        fprintf(stderr,
                "[NATIVE] invokePrivateLogger: methodObj is null\n");
        return;
    }

    /* ── 4. setAccessible(true) — unlock the private method ────────────
     *   Without this, invoke() throws IllegalAccessException.             */
    jclass  methodClass    = (*env)->GetObjectClass(env, methodObj);
    jmethodID setAccessMID = (*env)->GetMethodID(
                                 env, methodClass,
                                 "setAccessible", "(Z)V");
    if (setAccessMID)
        (*env)->CallVoidMethod(env, methodObj, setAccessMID, JNI_TRUE);

    /* ── 5. Build the Object[] args array for Method.invoke() ───────────
     *   Primitive int must be boxed as java.lang.Integer.                 */
    jmethodID intValueOfMID = (*env)->GetStaticMethodID(
                                  env, integerClass,
                                  "valueOf", "(I)Ljava/lang/Integer;");
    jobject   boxedCode     = (*env)->CallStaticObjectMethod(
                                  env, integerClass,
                                  intValueOfMID, errorCode);

    jstring   jMessage      = cToJstr(env, message);

    jclass    objectArrCls  = (*env)->FindClass(env, "java/lang/Object");
    jobjectArray invokeArgs =
        (*env)->NewObjectArray(env, 2, objectArrCls, NULL);
    (*env)->SetObjectArrayElement(env, invokeArgs, 0, boxedCode);
    (*env)->SetObjectArrayElement(env, invokeArgs, 1, (jobject)jMessage);

    /* ── 6. Method.invoke(processorObj, args) ───────────────────────────
     *   Signature: invoke(Object obj, Object... args)                     */
    jmethodID invokeMID = (*env)->GetMethodID(
                              env, methodClass,
                              "invoke",
                              "(Ljava/lang/Object;[Ljava/lang/Object;)"
                              "Ljava/lang/Object;");
    if (!invokeMID) {
        fprintf(stderr,
                "[NATIVE] invokePrivateLogger: Method.invoke not found\n");
        return;
    }

    (*env)->CallObjectMethod(env, methodObj, invokeMID,
                             processorObj, invokeArgs);

    /* ── 7. Check and clear any InvocationTargetException ──────────────── */
    if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        fprintf(stderr,
                "[NATIVE] invokePrivateLogger: "
                "exception during method invocation\n");
    }

    printf("[NATIVE] logNativeError invoked — "
           "code=%d, msg=\"%s\"\n", errorCode, message);
}

/* ══════════════════════════════════════════════════════════════════════════
 *  Utility: read processorId from the Java object (for logging)
 * ══════════════════════════════════════════════════════════════════════════ */
static const char *readProcessorId(JNIEnv *env, jobject procObj,
                                   jstring *outJstr)
{
    jclass    cls = (*env)->GetObjectClass(env, procObj);
    jmethodID mid = (*env)->GetMethodID(
                        env, cls, "getProcessorId",
                        "()Ljava/lang/String;");
    if (!mid) { *outJstr = NULL; return "<unknown>"; }
    *outJstr = (jstring)(*env)->CallObjectMethod(env, procObj, mid);
    return jstrToC(env, *outJstr);
}

/* ════════��═════════════════════════════════════════════════════════════════
 *  JNI: SensitiveDataProcessor.processData(byte[])
 * ══════════════════════════════════════════════════════════════════════════ */
JNIEXPORT jbyteArray JNICALL
Java_SensitiveDataProcessor_processData(JNIEnv  *env,
                                        jobject  procObj,
                                        jbyteArray jData)
{
    jstring     pidJstr = NULL;
    const char *pid     = readProcessorId(env, procObj, &pidJstr);
    printf("[NATIVE] processData() called — processor: %s\n", pid);
    releaseJstr(env, pidJstr, pid);

    /* ── Guard: null input ──────────────────────────────────────────────── */
    if (!jData) {
        invokePrivateLogger(env, procObj,
                            ERR_NULL_INPUT,
                            "processData() received a null input payload");
        return NULL;
    }

    /* ── Read payload bytes ─────────────────────────────────────────────── */
    jsize  len   = (*env)->GetArrayLength(env, jData);
    jbyte *elems = (*env)->GetByteArrayElements(env, jData, NULL);
    if (!elems) {
        invokePrivateLogger(env, procObj,
                            ERR_PROCESSING_FAILED,
                            "Failed to pin input byte array");
        return NULL;
    }

    /* ── Simulate processing: XOR each byte with 0xAB ─────────────────── */
    jbyte *processed = (jbyte *)malloc((size_t)len);
    if (!processed) {
        (*env)->ReleaseByteArrayElements(env, jData, elems, JNI_ABORT);
        invokePrivateLogger(env, procObj,
                            ERR_PROCESSING_FAILED,
                            "Memory allocation failed during processing");
        return NULL;
    }

    jsize i;
    for (i = 0; i < len; i++)
        processed[i] = (jbyte)(elems[i] ^ (jbyte)0xAB);

    (*env)->ReleaseByteArrayElements(env, jData, elems, JNI_ABORT);

    printf("[NATIVE] Processed %d bytes successfully.\n", (int)len);

    /* ── Return processed bytes to Java ────────────────────────────────── */
    jbyteArray result = (*env)->NewByteArray(env, len);
    if (!result) {
        free(processed);
        invokePrivateLogger(env, procObj,
                            ERR_PROCESSING_FAILED,
                            "Failed to allocate output byte array");
        return NULL;
    }

    (*env)->SetByteArrayRegion(env, result, 0, len, processed);
    free(processed);
    return result;
}

/* ══════════════════════════════════════════════════════════════════════════
 *  JNI: SensitiveDataProcessor.validateData(byte[], long)
 * ══════════════════════════════════════════════════════════════════════════ */
JNIEXPORT jboolean JNICALL
Java_SensitiveDataProcessor_validateData(JNIEnv    *env,
                                         jobject    procObj,
                                         jbyteArray jData,
                                         jlong      expectedChecksum)
{
    /* ── Guard: null input ──────────────────────────────────────────────── */
    if (!jData) {
        invokePrivateLogger(env, procObj,
                            ERR_NULL_INPUT,
                            "validateData() received a null data block");
        return JNI_FALSE;
    }

    /* ── Compute a simple checksum (sum of all bytes) ───────────────────── */
    jsize  len   = (*env)->GetArrayLength(env, jData);
    jbyte *elems = (*env)->GetByteArrayElements(env, jData, NULL);
    if (!elems) {
        invokePrivateLogger(env, procObj,
                            ERR_PROCESSING_FAILED,
                            "Failed to pin data block for checksum");
        return JNI_FALSE;
    }

    jlong computed = 0;
    jsize i;
    for (i = 0; i < len; i++)
        computed += (unsigned char)elems[i];

    (*env)->ReleaseByteArrayElements(env, jData, elems, JNI_ABORT);

    printf("[NATIVE] validateData: computed=%lld, expected=%lld\n",
           (long long)computed, (long long)expectedChecksum);

    /* ── Validate ───────────────────────────────────────────────────────── */
    if (computed != expectedChecksum) {
        char msg[128];
        snprintf(msg, sizeof(msg),
                 "Checksum mismatch: computed=%lld, expected=%lld",
                 (long long)computed, (long long)expectedChecksum);
        invokePrivateLogger(env, procObj, ERR_CHECKSUM_MISMATCH, msg);
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/* ══════════════════════════════════════════════════════════════════════════
 *  JNI: SensitiveDataProcessor.getNativeStatusReport()
 * ══════════════════════════════════════════════════════════════════════════ */
JNIEXPORT jstring JNICALL
Java_SensitiveDataProcessor_getNativeStatusReport(JNIEnv *env,
                                                  jobject procObj)
{
    jclass cls = (*env)->GetObjectClass(env, procObj);
    char   report[512];

    /* processorId */
    jstring     pidJstr = NULL;
    const char *pid     = readProcessorId(env, procObj, &pidJstr);

    /* errorCount */
    jmethodID getErrCnt =
        (*env)->GetMethodID(env, cls, "getErrorCount", "()I");
    jint errorCount = getErrCnt
        ? (*env)->CallIntMethod(env, procObj, getErrCnt)
        : -1;

    /* processingActive */
    jmethodID isActive =
        (*env)->GetMethodID(env, cls, "isProcessingActive", "()Z");
    jboolean active = isActive
        ? (*env)->CallBooleanMethod(env, procObj, isActive)
        : JNI_FALSE;

    snprintf(report, sizeof(report),
             "[NATIVE STATUS REPORT]\n"
             "  Processor ID      : %s\n"
             "  Error Count       : %d\n"
             "  Processing Active : %s\n",
             pid, (int)errorCount,
             active == JNI_TRUE ? "YES" : "NO");

    releaseJstr(env, pidJstr, pid);
    return cToJstr(env, report);
}