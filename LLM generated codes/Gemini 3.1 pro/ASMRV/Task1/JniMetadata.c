#include <jni.h>
#include <stdio.h>
#include <string.h>

// JNI Implementation for the getTargetClassMetadata method
JNIEXPORT jstring JNICALL Java_JniMetadata_getTargetClassMetadata(JNIEnv *env, jobject thisObj) {
    // 1. Locate the specific Java class
    jclass targetClass = (*env)->FindClass(env, "TargetClass");
    if (targetClass == NULL) {
        return (*env)->NewStringUTF(env, "Error: TargetClass not found");
    }

    // 2. Retrieve information related to it (via java.lang.Class methods)
    jclass classClass = (*env)->FindClass(env, "java/lang/Class");
    
    // Get the method IDs for Class.getName() and Class.isInterface()
    jmethodID midGetName = (*env)->GetMethodID(env, classClass, "getName", "()Ljava/lang/String;");
    jmethodID midIsInterface = (*env)->GetMethodID(env, classClass, "isInterface", "()Z");

    if (midGetName == NULL || midIsInterface == NULL) {
        return (*env)->NewStringUTF(env, "Error: Could not find Class methods");
    }

    // Invoke the methods on our targetClass reference
    jstring nameObj = (jstring) (*env)->CallObjectMethod(env, targetClass, midGetName);
    jboolean isInterface = (*env)->CallBooleanMethod(env, targetClass, midIsInterface);

    // Convert the Java string to a C string
    const char *cName = (*env)->GetStringUTFChars(env, nameObj, NULL);

    // Format the extracted metadata into a buffer
    char buffer[256];
    snprintf(buffer, sizeof(buffer), "Class Name: '%s' | Type: %s", 
             cName, 
             (isInterface == JNI_TRUE) ? "Interface" : "Standard Class");

    // Release JNI resources to prevent memory leaks
    (*env)->ReleaseStringUTFChars(env, nameObj, cName);

    // 3. Return the retrieved metadata information to Java
    return (*env)->NewStringUTF(env, buffer);
}