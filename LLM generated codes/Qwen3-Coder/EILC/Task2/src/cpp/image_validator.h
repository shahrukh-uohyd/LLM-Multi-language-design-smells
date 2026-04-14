// image_validator.h
#ifndef IMAGE_VALIDATOR_H
#define IMAGE_VALIDATOR_H

#include <jni.h>
#include <string>
#include <vector>

#ifdef __cplusplus
extern "C" {
#endif

// Individual validation methods
JNIEXPORT jboolean JNICALL Java_ImageValidationService_validateImageHeader
  (JNIEnv *, jobject, jstring);

JNIEXPORT jboolean JNICALL Java_ImageValidationService_isSupportedFormat
  (JNIEnv *, jobject, jstring);

JNIEXPORT jstring JNICALL Java_ImageValidationService_getValidationError
  (JNIEnv *, jobject, jstring);

// Batch validation method
JNIEXPORT jobjectArray JNICALL Java_ImageValidationService_validateImageBatch
  (JNIEnv *, jobject, jobjectArray);

#ifdef __cplusplus
}
#endif

#endif