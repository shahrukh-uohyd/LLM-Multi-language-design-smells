#include <jni.h>
#include <stdint.h>
#include <stddef.h>

JNIEXPORT jbyteArray JNICALL
Java_DocumentMetadata_generateCompactHeader(JNIEnv *env, jobject thisObj) {
    
    // 1. Get the class of the object
    jclass metaClass = (*env)->GetObjectClass(env, thisObj);
    if (metaClass == NULL) return NULL;

    // 2. Get Field IDs
    // "I" = int, "J" = long, "Z" = boolean
    jfieldID ownerIDField = (*env)->GetFieldID(env, metaClass, "ownerID", "I");
    jfieldID timestampField = (*env)->GetFieldID(env, metaClass, "creationTimestamp", "J");
    jfieldID encryptedField = (*env)->GetFieldID(env, metaClass, "isEncrypted", "Z");
    jfieldID permsField = (*env)->GetFieldID(env, metaClass, "filePermissions", "I");

    if (ownerIDField == NULL || timestampField == NULL || encryptedField == NULL || permsField == NULL) {
        return NULL; // Field not found
    }

    // 3. Extract values from the Java object
    jint ownerID = (*env)->GetIntField(env, thisObj, ownerIDField);
    jlong creationTimestamp = (*env)->GetLongField(env, thisObj, timestampField);
    jboolean isEncrypted = (*env)->GetBooleanField(env, thisObj, encryptedField);
    jint filePermissions = (*env)->GetIntField(env, thisObj, permsField);

    // 4. Pack into a compact binary buffer (Big-Endian format)
    // 4 bytes (int) + 8 bytes (long) + 1 byte (boolean) + 4 bytes (int) = 17 bytes total
    uint8_t buffer[17];
    int offset = 0;

    // Pack ownerID
    buffer[offset++] = (uint8_t)((ownerID >> 24) & 0xFF);
    buffer[offset++] = (uint8_t)((ownerID >> 16) & 0xFF);
    buffer[offset++] = (uint8_t)((ownerID >> 8) & 0xFF);
    buffer[offset++] = (uint8_t)(ownerID & 0xFF);

    // Pack creationTimestamp
    buffer[offset++] = (uint8_t)((creationTimestamp >> 56) & 0xFF);
    buffer[offset++] = (uint8_t)((creationTimestamp >> 48) & 0xFF);
    buffer[offset++] = (uint8_t)((creationTimestamp >> 40) & 0xFF);
    buffer[offset++] = (uint8_t)((creationTimestamp >> 32) & 0xFF);
    buffer[offset++] = (uint8_t)((creationTimestamp >> 24) & 0xFF);
    buffer[offset++] = (uint8_t)((creationTimestamp >> 16) & 0xFF);
    buffer[offset++] = (uint8_t)((creationTimestamp >> 8) & 0xFF);
    buffer[offset++] = (uint8_t)(creationTimestamp & 0xFF);

    // Pack isEncrypted
    buffer[offset++] = (isEncrypted == JNI_TRUE) ? 1 : 0;

    // Pack filePermissions
    buffer[offset++] = (uint8_t)((filePermissions >> 24) & 0xFF);
    buffer[offset++] = (uint8_t)((filePermissions >> 16) & 0xFF);
    buffer[offset++] = (uint8_t)((filePermissions >> 8) & 0xFF);
    buffer[offset++] = (uint8_t)(filePermissions & 0xFF);

    // 5. Create a Java byte array and copy the buffer into it
    jbyteArray headerArray = (*env)->NewByteArray(env, 17);
    if (headerArray != NULL) {
        (*env)->SetByteArrayRegion(env, headerArray, 0, 17, (const jbyte *)buffer);
    }

    return headerArray;
}