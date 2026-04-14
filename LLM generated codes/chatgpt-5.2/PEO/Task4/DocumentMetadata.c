#include <jni.h>
#include <stdint.h>
#include "DocumentMetadata.h"

/*
 * byte[] toBinaryHeader()
 */
JNIEXPORT jbyteArray JNICALL
Java_DocumentMetadata_toBinaryHeader(JNIEnv *env,
                                     jobject obj) {

    jclass cls = (*env)->GetObjectClass(env, obj);
    if (cls == NULL) {
        return NULL;
    }

    // Field IDs
    jfieldID ownerIDField =
        (*env)->GetFieldID(env, cls, "ownerID", "I");
    jfieldID timestampField =
        (*env)->GetFieldID(env, cls, "creationTimestamp", "J");
    jfieldID encryptedField =
        (*env)->GetFieldID(env, cls, "isEncrypted", "Z");
    jfieldID permissionsField =
        (*env)->GetFieldID(env, cls, "filePermissions", "I");

    if (ownerIDField == NULL ||
        timestampField == NULL ||
        encryptedField == NULL ||
        permissionsField == NULL) {
        (*env)->ExceptionClear(env);
        return NULL;
    }

    // Read values
    jint ownerID =
        (*env)->GetIntField(env, obj, ownerIDField);
    jlong timestamp =
        (*env)->GetLongField(env, obj, timestampField);
    jboolean isEncrypted =
        (*env)->GetBooleanField(env, obj, encryptedField);
    jint permissions =
        (*env)->GetIntField(env, obj, permissionsField);

    // Allocate result: 4 + 8 + 1 + 4 = 17 bytes
    const int HEADER_SIZE = 17;
    jbyteArray result =
        (*env)->NewByteArray(env, HEADER_SIZE);
    if (result == NULL) {
        return NULL;
    }

    jbyte buffer[HEADER_SIZE];

    // Pack ownerID (int, big-endian)
    buffer[0] = (jbyte)((ownerID >> 24) & 0xFF);
    buffer[1] = (jbyte)((ownerID >> 16) & 0xFF);
    buffer[2] = (jbyte)((ownerID >> 8) & 0xFF);
    buffer[3] = (jbyte)(ownerID & 0xFF);

    // Pack creationTimestamp (long, big-endian)
    buffer[4]  = (jbyte)((timestamp >> 56) & 0xFF);
    buffer[5]  = (jbyte)((timestamp >> 48) & 0xFF);
    buffer[6]  = (jbyte)((timestamp >> 40) & 0xFF);
    buffer[7]  = (jbyte)((timestamp >> 32) & 0xFF);
    buffer[8]  = (jbyte)((timestamp >> 24) & 0xFF);
    buffer[9]  = (jbyte)((timestamp >> 16) & 0xFF);
    buffer[10] = (jbyte)((timestamp >> 8) & 0xFF);
    buffer[11] = (jbyte)(timestamp & 0xFF);

    // Pack isEncrypted (1 byte)
    buffer[12] = (isEncrypted == JNI_TRUE) ? 1 : 0;

    // Pack filePermissions (int, big-endian)
    buffer[13] = (jbyte)((permissions >> 24) & 0xFF);
    buffer[14] = (jbyte)((permissions >> 16) & 0xFF);
    buffer[15] = (jbyte)((permissions >> 8) & 0xFF);
    buffer[16] = (jbyte)(permissions & 0xFF);

    // Copy into Java byte[]
    (*env)->SetByteArrayRegion(env, result, 0, HEADER_SIZE, buffer);
    return result;
}
