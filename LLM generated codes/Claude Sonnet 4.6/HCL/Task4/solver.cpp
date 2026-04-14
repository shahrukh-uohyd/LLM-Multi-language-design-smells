#include "solver.h"
#include <cmath>
#include <cstring>
#include <stdexcept>
#include <vector>

// ─── Internal solver state ────────────────────────────────────────────────────
struct SolverState {
    int    maxIterations;
    double tolerance;
    double lastResidual;
    int    lastIterations;
};

// ─── Gauss-Seidel iterative solver ───────────────────────────────────────────
static bool gaussSeidel(
        const double* A, const double* b, double* x, int n,
        int maxIter, double tol, double& residual, int& iters) {

    std::memset(x, 0, n * sizeof(double));

    for (iters = 0; iters < maxIter; iters++) {
        double maxDelta = 0.0;
        for (int i = 0; i < n; i++) {
            double sigma = 0.0;
            for (int j = 0; j < n; j++) {
                if (j != i) sigma += A[i * n + j] * x[j];
            }
            double xNew = (b[i] - sigma) / A[i * n + i];
            maxDelta = std::max(maxDelta, std::abs(xNew - x[i]));
            x[i] = xNew;
        }
        residual = maxDelta;
        if (maxDelta < tol) return true;  // converged
    }
    return false;
}

// ─── JNI implementation ───────────────────────────────────────────────────────

JNIEXPORT jlong JNICALL
Java_com_solver_NativeSolver_nativeInitialize(JNIEnv* env, jobject obj,
        jint maxIterations, jdouble tolerance) {
    auto* state = new SolverState{
        static_cast<int>(maxIterations),
        static_cast<double>(tolerance),
        0.0, 0
    };
    return reinterpret_cast<jlong>(state);
}

JNIEXPORT jboolean JNICALL
Java_com_solver_NativeSolver_nativeSolve(JNIEnv* env, jobject obj,
        jlong handle, jdoubleArray flatA, jdoubleArray b,
        jdoubleArray solution, jint n) {

    auto* state = reinterpret_cast<SolverState*>(handle);
    jdouble* A_ptr = env->GetDoubleArrayElements(flatA,    nullptr);
    jdouble* b_ptr = env->GetDoubleArrayElements(b,        nullptr);
    jdouble* x_ptr = env->GetDoubleArrayElements(solution, nullptr);

    bool converged = gaussSeidel(
        A_ptr, b_ptr, x_ptr, static_cast<int>(n),
        state->maxIterations, state->tolerance,
        state->lastResidual, state->lastIterations
    );

    env->ReleaseDoubleArrayElements(flatA,    A_ptr, JNI_ABORT);
    env->ReleaseDoubleArrayElements(b,        b_ptr, JNI_ABORT);
    env->ReleaseDoubleArrayElements(solution, x_ptr, 0);         // write back

    return converged ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jdouble JNICALL
Java_com_solver_NativeSolver_nativeGetLastResidual(JNIEnv* env, jobject obj, jlong handle) {
    return reinterpret_cast<SolverState*>(handle)->lastResidual;
}

JNIEXPORT jint JNICALL
Java_com_solver_NativeSolver_nativeGetIterations(JNIEnv* env, jobject obj, jlong handle) {
    return reinterpret_cast<SolverState*>(handle)->lastIterations;
}

JNIEXPORT jstring JNICALL
Java_com_solver_NativeSolver_nativeGetVersion(JNIEnv* env, jobject obj, jlong handle) {
    return env->NewStringUTF("solver-native/1.0.0");
}

JNIEXPORT void JNICALL
Java_com_solver_NativeSolver_nativeShutdown(JNIEnv* env, jobject obj, jlong handle) {
    delete reinterpret_cast<SolverState*>(handle);
}