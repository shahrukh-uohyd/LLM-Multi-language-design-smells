#include <jni.h>
#include <stdio.h>

JNIEXPORT void JNICALL Java_SecureDataProcessor_processNativeData(JNIEnv *env, jobject thiz) {
    
    // ... [Simulated native data processing operations] ...
    
    int nativeErrorCode = -1; // Simulating a native error condition

    if (nativeErrorCode != 0) {
        // --- ERROR OCCURRED: INVOKE JAVA LOGGER ---

        // 1. Get the class of the Java object
        jclass processorClass = (*env)->GetObjectClass(env, thiz);
        if (processorClass == NULL) {
            return;
        }

        // 2. Find the Method ID of the private logging method
        // Method Name: "logSecureDiagnostic"
        // JNI Signature: "(Ljava/lang/String;)V" (Takes a String parameter, returns void)
        jmethodID logMethodId = (*env)->GetMethodID(env, processorClass, "logSecureDiagnostic", "(Ljava/lang/String;)V");
        
        if (logMethodId == NULL) {
            // If the method doesn't exist, JNI prepares a NoSuchMethodError for the JVM.
            (*env)->DeleteLocalRef(env, processorClass);
            return;
        }

        // 3. Create a Java String containing the native error message
        const char* errorDetail = "Native Error [-1]: Secure memory allocation failed during processing.";
        jstring jErrorMessage = (*env)->NewStringUTF(env, errorDetail);
        
        if (jErrorMessage == NULL) {
            // Out of memory error when creating the string
            (*env)->DeleteLocalRef(env, processorClass);
            return;
        }

        // 4. Invoke the private Java method
        // We use CallVoidMethod because the Java method returns 'void'
        (*env)->CallVoidMethod(env, thiz, logMethodId, jErrorMessage);

        // 5. Clean up local JNI references to prevent memory leaks
        (*env)->DeleteLocalRef(env, jErrorMessage);
        (*env)->DeleteLocalRef(env, processorClass);
        
        // ... [Handle native cleanup and abort processing] ...
    }
}