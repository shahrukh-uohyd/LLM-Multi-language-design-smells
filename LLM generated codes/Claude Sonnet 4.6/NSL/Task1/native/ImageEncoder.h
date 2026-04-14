/* DO NOT EDIT – matches javac -h output for the default-package ImageEncoder */
#include <jni.h>

#ifndef _Included_ImageEncoder
#define _Included_ImageEncoder
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jstring JNICALL
Java_ImageEncoder_encodeToBase64(JNIEnv *, jobject, jbyteArray, jint, jint, jint);

JNIEXPORT jbyteArray JNICALL
Java_ImageEncoder_encodeWithRLE(JNIEnv *, jobject, jbyteArray, jint, jint, jint);

JNIEXPORT jstring JNICALL
Java_ImageEncoder_encodeToHex(JNIEnv *, jobject, jbyteArray, jint, jint, jint);

JNIEXPORT jstring JNICALL
Java_ImageEncoder_getNativeLibraryInfo(JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif