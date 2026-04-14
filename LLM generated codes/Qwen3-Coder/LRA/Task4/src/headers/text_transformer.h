// src/headers/text_transformer.h
#ifndef TEXT_TRANSFORMER_H
#define TEXT_TRANSFORMER_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     TextTransformer
 * Method:    transformToUppercase
 * Signature: ([Ljava/lang/String;)[Ljava/lang/String;
 */
JNIEXPORT jobjectArray JNICALL Java_TextTransformer_transformToUppercase
  (JNIEnv *, jobject, jobjectArray);

#ifdef __cplusplus
}
#endif

#endif // TEXT_TRANSFORMER_H