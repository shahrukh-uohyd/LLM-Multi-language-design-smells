// src/headers/data_analysis.h
#ifndef DATA_ANALYSIS_H
#define DATA_ANALYSIS_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     DataAnalysisTool
 * Method:    processUserRecords
 * Signature: ([LUserRecord;)I
 */
JNIEXPORT jint JNICALL Java_DataAnalysisTool_processUserRecords
  (JNIEnv *, jobject, jobjectArray);

/*
 * Class:     DataAnalysisTool
 * Method:    processUserRecordsOptimized
 * Signature: ([LUserRecord;)J
 */
JNIEXPORT jlong JNICALL Java_DataAnalysisTool_processUserRecordsOptimized
  (JNIEnv *, jobject, jobjectArray);

/*
 * Class:     DataAnalysisTool
 * Method:    processUserRecordsWithStats
 * Signature: ([LUserRecord;)[I
 */
JNIEXPORT jintArray JNICALL Java_DataAnalysisTool_processUserRecordsWithStats
  (JNIEnv *, jobject, jobjectArray);

#ifdef __cplusplus
}
#endif

#endif // DATA_ANALYSIS_H