// database_bridge.h
#ifndef DATABASE_BRIDGE_H
#define DATABASE_BRIDGE_H

#include <jni.h>
#include <string>
#include <vector>

#ifdef __cplusplus
extern "C" {
#endif

// Individual record commit
JNIEXPORT jboolean JNICALL Java_DatabaseBridge_commitToStorage
  (JNIEnv *, jobject, jstring, jstring, jdouble, jstring, jlong, jstring, jstring);

// Batch individual commit (optimized)
JNIEXPORT jobjectArray JNICALL Java_DatabaseBridge_commitRecordsIndividually
  (JNIEnv *, jobject, jobjectArray, jobjectArray, jdoubleArray, jobjectArray, 
   jlongArray, jobjectArray, jobjectArray);

#ifdef __cplusplus
}
#endif

#endif