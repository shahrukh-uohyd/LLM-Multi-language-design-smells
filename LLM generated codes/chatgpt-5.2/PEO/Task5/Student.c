#include <jni.h>
#include "Student.h"

/*
 * double computeAverageGrade()
 */
JNIEXPORT jdouble JNICALL
Java_Student_computeAverageGrade(JNIEnv *env,
                                 jobject obj) {

    // Get the Student class
    jclass cls = (*env)->GetObjectClass(env, obj);
    if (cls == NULL) {
        return 0.0;
    }

    // Get field IDs for the grade fields
    jfieldID mathField =
        (*env)->GetFieldID(env, cls, "mathGrade", "D");
    jfieldID scienceField =
        (*env)->GetFieldID(env, cls, "scienceGrade", "D");
    jfieldID literatureField =
        (*env)->GetFieldID(env, cls, "literatureGrade", "D");

    if (mathField == NULL ||
        scienceField == NULL ||
        literatureField == NULL) {
        (*env)->ExceptionClear(env);
        return 0.0;
    }

    // Read grade values
    jdouble math =
        (*env)->GetDoubleField(env, obj, mathField);
    jdouble science =
        (*env)->GetDoubleField(env, obj, scienceField);
    jdouble literature =
        (*env)->GetDoubleField(env, obj, literatureField);

    // Compute average
    return (math + science + literature) / 3.0;
}
