#include <jni.h>
#include "DocumentMetadata.h"
#include <stdlib.h>
#include <string.h>

// Define the binary header structure size
// int (4 bytes) + long (8 bytes) + boolean (1 byte) + int (4 bytes) + padding (3 bytes) = 20 bytes total
#define BINARY_HEADER_SIZE 20

/*
 * Class:     DocumentMetadata
 * Method:    serializeToBinaryHeader
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL Java_DocumentMetadata_serializeToBinaryHeader
  (JNIEnv *env, jobject thisObj) {
    
    // Get the DocumentMetadata class
    jclass metadataClass = (*env)->GetObjectClass(env, thisObj);
    
    // Find field IDs
    jfieldID ownerIDFieldID = (*env)->GetFieldID(env, metadataClass, "ownerID", "I");
    jfieldID creationTimestampFieldID = (*env)->GetFieldID(env, metadataClass, "creationTimestamp", "J");
    jfieldID isEncryptedFieldID = (*env)->GetFieldID(env, metadataClass, "isEncrypted", "Z");
    jfieldID filePermissionsFieldID = (*env)->GetFieldID(env, metadataClass, "filePermissions", "I");
    
    if (!ownerIDFieldID || !creationTimestampFieldID || !isEncryptedFieldID || !filePermissionsFieldID) {
        (*env)->ExceptionDescribe(env);
        return NULL;
    }
    
    // Get field values
    jint ownerID = (*env)->GetIntField(env, thisObj, ownerIDFieldID);
    jlong creationTimestamp = (*env)->GetLongField(env, thisObj, creationTimestampFieldID);
    jboolean isEncrypted = (*env)->GetBooleanField(env, thisObj, isEncryptedFieldID);
    jint filePermissions = (*env)->GetIntField(env, thisObj, filePermissionsFieldID);
    
    // Allocate memory for the binary header
    unsigned char* headerData = (unsigned char*)malloc(BINARY_HEADER_SIZE);
    if (!headerData) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "OutOfMemoryError"), "Failed to allocate header buffer");
        return NULL;
    }
    
    // Pack the data into the binary header (little-endian format)
    int offset = 0;
    
    // Write ownerID (4 bytes)
    *(jint*)(headerData + offset) = ownerID;
    offset += sizeof(jint);
    
    // Write creationTimestamp (8 bytes)
    *(jlong*)(headerData + offset) = creationTimestamp;
    offset += sizeof(jlong);
    
    // Write isEncrypted (1 byte) - convert boolean to 1/0
    *(char*)(headerData + offset) = (char)(isEncrypted ? 1 : 0);
    offset += sizeof(char);
    
    // Write filePermissions (4 bytes)
    *(jint*)(headerData + offset) = filePermissions;
    // Note: We have 3 bytes of padding at the end to maintain alignment
    
    // Create a Java byte array to return
    jbyteArray result = (*env)->NewByteArray(env, BINARY_HEADER_SIZE);
    if (result == NULL) {
        free(headerData);
        return NULL; // OutOfMemoryError thrown
    }
    
    // Copy the binary data to the Java array
    (*env)->SetByteArrayRegion(env, result, 0, BINARY_HEADER_SIZE, (jbyte*)headerData);
    
    // Clean up
    free(headerData);
    
    return result;
}

/*
 * Class:     DocumentMetadata
 * Method:    deserializeFromBinaryHeader
 * Signature: ([B)V
 */
JNIEXPORT void JNICALL Java_DocumentMetadata_deserializeFromBinaryHeader
  (JNIEnv *env, jobject thisObj, jbyteArray headerData) {
    
    if (headerData == NULL) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "IllegalArgumentException"), "Header data cannot be null");
        return;
    }
    
    // Get array length
    jsize arrayLength = (*env)->GetArrayLength(env, headerData);
    if (arrayLength != BINARY_HEADER_SIZE) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "IllegalArgumentException"), 
                        "Invalid header size, expected 20 bytes");
        return;
    }
    
    // Get the DocumentMetadata class
    jclass metadataClass = (*env)->GetObjectClass(env, thisObj);
    
    // Find field IDs
    jfieldID ownerIDFieldID = (*env)->GetFieldID(env, metadataClass, "ownerID", "I");
    jfieldID creationTimestampFieldID = (*env)->GetFieldID(env, metadataClass, "creationTimestamp", "J");
    jfieldID isEncryptedFieldID = (*env)->GetFieldID(env, metadataClass, "isEncrypted", "Z");
    jfieldID filePermissionsFieldID = (*env)->GetFieldID(env, metadataClass, "filePermissions", "I");
    
    if (!ownerIDFieldID || !creationTimestampFieldID || !isEncryptedFieldID || !filePermissionsFieldID) {
        (*env)->ExceptionDescribe(env);
        return;
    }
    
    // Get the byte array elements
    jbyte* dataArray = (*env)->GetByteArrayElements(env, headerData, NULL);
    if (dataArray == NULL) {
        return; // OutOfMemoryError thrown
    }
    
    // Extract data from the binary header
    int offset = 0;
    
    // Read ownerID (4 bytes)
    jint ownerID = *(jint*)(dataArray + offset);
    offset += sizeof(jint);
    
    // Read creationTimestamp (8 bytes)
    jlong creationTimestamp = *(jlong*)(dataArray + offset);
    offset += sizeof(jlong);
    
    // Read isEncrypted (1 byte)
    jboolean isEncrypted = (jboolean)(*(char*)(dataArray + offset));
    offset += sizeof(char);
    
    // Read filePermissions (4 bytes)
    jint filePermissions = *(jint*)(dataArray + offset);
    
    // Release the array elements
    (*env)->ReleaseByteArrayElements(env, headerData, dataArray, JNI_ABORT);
    
    // Set the field values
    (*env)->SetIntField(env, thisObj, ownerIDFieldID, ownerID);
    (*env)->SetLongField(env, thisObj, creationTimestampFieldID, creationTimestamp);
    (*env)->SetBooleanField(env, thisObj, isEncryptedFieldID, isEncrypted);
    (*env)->SetIntField(env, thisObj, filePermissionsFieldID, filePermissions);
}