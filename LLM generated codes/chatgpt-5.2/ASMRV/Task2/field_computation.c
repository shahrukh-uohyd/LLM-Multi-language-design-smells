// File: field_computation.c
#include <jni.h>

JNIEXPORT jdouble JNICALL
Java_FieldComputation_computeTotalEarnings(JNIEnv *env, jobject obj, jobject employeeObj) {

    // Get the Employee class
    jclass employeeClass = (*env)->GetObjectClass(env, employeeObj);
    if (employeeClass == NULL) {
        return 0.0;
    }

    // Locate fields
    jfieldID yearsField = (*env)->GetFieldID(env, employeeClass,
                                             "yearsWorked", "I");
    jfieldID salaryField = (*env)->GetFieldID(env, employeeClass,
                                              "salary", "D");

    if (yearsField == NULL || salaryField == NULL) {
        return 0.0;
    }

    // Read field values
    jint yearsWorked = (*env)->GetIntField(env, employeeObj, yearsField);
    jdouble salary = (*env)->GetDoubleField(env, employeeObj, salaryField);

    // Compute derived value
    jdouble totalEarnings = yearsWorked * salary;

    return totalEarnings;
}
