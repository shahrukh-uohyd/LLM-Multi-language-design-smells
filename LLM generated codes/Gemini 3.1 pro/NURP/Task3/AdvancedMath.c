#include <jni.h>
#include <math.h>
#include "AdvancedMath.h"

// Implementation of the native power method
JNIEXPORT jdouble JNICALL Java_AdvancedMath_power
  (JNIEnv *env, jobject obj, jdouble base, jdouble exponent) {
    // Utilize the C standard math library
    return pow(base, exponent);
}

// Implementation of the native factorial method
JNIEXPORT jlong JNICALL Java_AdvancedMath_factorial
  (JNIEnv *env, jobject obj, jint n) {
    if (n < 0) return -1; // Error case
    if (n == 0 || n == 1) return 1;
    
    jlong result = 1;
    for (int i = 2; i <= n; i++) {
        result *= i;
    }
    return result;
}

// Implementation of the native fibonacci method
JNIEXPORT jlong JNICALL Java_AdvancedMath_fibonacci
  (JNIEnv *env, jobject obj, jint n) {
    if (n <= 0) return 0;
    if (n == 1) return 1;
    
    jlong a = 0;
    jlong b = 1;
    jlong temp;
    
    for (int i = 2; i <= n; i++) {
        temp = a + b;
        a = b;
        b = temp;
    }
    return b;
}