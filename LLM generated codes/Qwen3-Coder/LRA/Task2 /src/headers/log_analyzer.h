// src/headers/log_analyzer.h
#ifndef LOG_ANALYZER_H
#define LOG_ANALYZER_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/*
 * Class:     LogAnalyzer
 * Method:    analyzeLogEventsBySeverity
 * Signature: ([LLogEvent;)[I
 */
JNIEXPORT jintArray JNICALL Java_LogAnalyzer_analyzeLogEventsBySeverity
  (JNIEnv *, jobject, jobjectArray);

/*
 * Class:     LogAnalyzer
 * Method:    analyzeLogEventsByComponent
 * Signature: ([LLogEvent;)Ljava/util/HashMap;
 */
JNIEXPORT jobject JNICALL Java_LogAnalyzer_analyzeLogEventsByComponent
  (JNIEnv *, jobject, jobjectArray);

/*
 * Class:     LogAnalyzer
 * Method:    analyzeLogEventsWithTimeRange
 * Signature: ([LLogEvent;Ljava/time/LocalDateTime;Ljava/time/LocalDateTime;)[J
 */
JNIEXPORT jlongArray JNICALL Java_LogAnalyzer_analyzeLogEventsWithTimeRange
  (JNIEnv *, jobject, jobjectArray, jobject, jobject);

#ifdef __cplusplus
}
#endif

#endif // LOG_ANALYZER_H