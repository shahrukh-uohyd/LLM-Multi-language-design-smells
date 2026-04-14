#include <jni.h>
#include <math.h>     /* isfinite() — input validation                      */
#include <stddef.h>   /* NULL                                               */
#include "Student.h"  /* generated via: javac -h . Student.java             */

/* -------------------------------------------------------------------------
 * GRADE_COUNT: number of subjects used in the average.
 * Defined as a constant so the divisor is never a magic number.
 * ------------------------------------------------------------------------- */
#define GRADE_COUNT 3

/* -------------------------------------------------------------------------
 * compute_average:
 * Pure C logic — zero JNI dependency.
 * Isolated so it can be unit-tested without a JVM.
 *
 * Returns:
 *   The arithmetic mean of the three grades  on success.
 *  -1.0                                       if any input is non-finite
 *                                             (NaN, Inf) — sentinel for
 *                                             invalid input.
 * ------------------------------------------------------------------------- */
static double compute_average(double mathGrade,
                               double scienceGrade,
                               double literatureGrade) {

    /* --- Guard: reject non-finite inputs --- */
    /* A corrupted or uninitialised grade could be NaN or Infinity.         */
    /* Propagating such values silently into an average is a data integrity  */
    /* error — fail explicitly instead.                                      */
    if (!isfinite(mathGrade) ||
        !isfinite(scienceGrade) ||
        !isfinite(literatureGrade)) {
        return -1.0;   /* Sentinel: caller must treat this as invalid input */
    }

    return (mathGrade + scienceGrade + literatureGrade) / (double)GRADE_COUNT;
}


/* -------------------------------------------------------------------------
 * APPROACH A (Less Preferred):
 * Receives the full Student jobject.
 * Reads ONLY the three grade fields — name and rollNumber are deliberately
 * never touched, but the compiler cannot enforce that constraint.
 *
 * Additional maintenance risk: field names are stringly-typed strings.
 * A rename in Java without updating these literals = NoSuchFieldError
 * at runtime, not at compile time.
 * ------------------------------------------------------------------------- */
JNIEXPORT jdouble JNICALL
Java_Student_computeAverageGrade(JNIEnv *env, jobject thisObj) {

    /* --- Step 1: Resolve the class --- */
    jclass studentClass = (*env)->GetObjectClass(env, thisObj);
    if (studentClass == NULL) {
        return -1.0;    /* Fail safely */
    }

    /* --- Step 2: Resolve field IDs for the three grade fields ONLY ---
     * JNI type descriptor for 'double' is "D".
     * name (String) and rollNumber (int) field IDs are deliberately
     * never requested — this is the only enforcement mechanism in Approach A. */
    jfieldID fid_math    = (*env)->GetFieldID(env, studentClass, "mathGrade",       "D");
    jfieldID fid_science = (*env)->GetFieldID(env, studentClass, "scienceGrade",    "D");
    jfieldID fid_lit     = (*env)->GetFieldID(env, studentClass, "literatureGrade", "D");

    if (fid_math == NULL || fid_science == NULL || fid_lit == NULL) {
        return -1.0;    /* Field resolution failed — fail safely */
    }

    /* --- Step 3: Read ONLY the three grade fields ---
     * 'name' and 'rollNumber' are never accessed. */
    jdouble mathGrade       = (*env)->GetDoubleField(env, thisObj, fid_math);
    jdouble scienceGrade    = (*env)->GetDoubleField(env, thisObj, fid_science);
    jdouble literatureGrade = (*env)->GetDoubleField(env, thisObj, fid_lit);

    /* --- Step 4: Delegate to the pure-C computation --- */
    return (jdouble)compute_average(mathGrade, scienceGrade, literatureGrade);
}


/* -------------------------------------------------------------------------
 * APPROACH B (RECOMMENDED — Principle of Least Privilege):
 * Java's computeAverageGradeSecure() extracted the three grade doubles
 * before crossing the JNI boundary. 'thisObj' is the implicit Java 'this'
 * reference — deliberately unused and marked as such.
 *
 * 'name' (PII) and 'rollNumber' (PII) are structurally unreachable here.
 * No GetFieldID calls. No stringly-typed field name strings.
 * The compute_average() helper is fully testable in pure C.
 * ------------------------------------------------------------------------- */
JNIEXPORT jdouble JNICALL
Java_Student_nativeComputeAverageGrade(JNIEnv *env, jobject thisObj,  /* thisObj unused */
                                        jdouble mathGrade,
                                        jdouble scienceGrade,
                                        jdouble literatureGrade) {

    /* No object introspection. No field lookups. No PII in scope.           */
    /* The computation receives exactly the three values it needs — nothing more. */
    (void)thisObj;   /* Explicitly suppress unused-parameter warning         */

    return (jdouble)compute_average(mathGrade, scienceGrade, literatureGrade);
}