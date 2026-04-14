#include "Solver.h"

JNIEXPORT jint JNICALL
Java_Solver_nativeSolve(JNIEnv *env, jobject obj, jint input) {
    // Example native solver logic
    return input * 2;
}
