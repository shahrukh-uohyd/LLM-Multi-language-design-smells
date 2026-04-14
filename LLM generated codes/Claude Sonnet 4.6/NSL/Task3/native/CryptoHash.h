/* Auto-generated equivalent of: javac -h native CryptoHash.java
   Default-package class → no package prefix in symbol names.          */
#include <jni.h>

#ifndef _Included_CryptoHash
#define _Included_CryptoHash
#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jbyteArray JNICALL Java_CryptoHash_computeMD5    (JNIEnv *, jobject, jbyteArray);
JNIEXPORT jbyteArray JNICALL Java_CryptoHash_computeSHA1   (JNIEnv *, jobject, jbyteArray);
JNIEXPORT jbyteArray JNICALL Java_CryptoHash_computeSHA256 (JNIEnv *, jobject, jbyteArray);
JNIEXPORT jbyteArray JNICALL Java_CryptoHash_computeSHA512 (JNIEnv *, jobject, jbyteArray);
JNIEXPORT jstring    JNICALL Java_CryptoHash_getNativeLibraryInfo(JNIEnv *, jobject);

#ifdef __cplusplus
}
#endif
#endif