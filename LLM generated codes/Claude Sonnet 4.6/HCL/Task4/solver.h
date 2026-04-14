#ifndef SOLVER_H
#define SOLVER_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jlong    JNICALL Java_com_solver_NativeSolver_nativeInitialize
  (JNIEnv*, jobject, jint, jdouble);

JNIEXPORT jboolean JNICALL Java_com_solver_NativeSolver_nativeSolve
  (JNIEnv*, jobject, jlong, jdoubleArray, jdoubleArray, jdoubleArray, jint);

JNIEXPORT jdouble  JNICALL Java_com_solver_NativeSolver_nativeGetLastResidual
  (JNIEnv*, jobject, jlong);

JNIEXPORT jint     JNICALL Java_com_solver_NativeSolver_nativeGetIterations
  (JNIEnv*, jobject, jlong);

JNIEXPORT jstring  JNICALL Java_com_solver_NativeSolver_nativeGetVersion
  (JNIEnv*, jobject, jlong);

JNIEXPORT void     JNICALL Java_com_solver_NativeSolver_nativeShutdown
  (JNIEnv*, jobject, jlong);

#ifdef __cplusplus
}
#endif
#endif /* SOLVER_H */