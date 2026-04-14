#include <jni.h>
#include <math.h>
#include "MathNative.h"

JNIEXPORT jdouble JNICALL
Java_MathNative_fastPower(JNIEnv *env, jclass cls, jdouble base, jdouble exp) {
    return pow(base, exp);
}

JNIEXPORT jdouble JNICALL
Java_MathNative_vectorMagnitude(JNIEnv *env, jclass cls,
                                jdouble x, jdouble y, jdouble z) {
    return sqrt(x*x + y*y + z*z);
}
