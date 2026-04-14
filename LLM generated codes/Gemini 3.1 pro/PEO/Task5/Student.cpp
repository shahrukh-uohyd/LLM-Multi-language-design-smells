#include <jni.h>

extern "C"
JNIEXPORT jdouble JNICALL
Java_Student_computeAverageGrade(JNIEnv *env, jobject thisObj) {
    
    // Safety check
    if (thisObj == nullptr) {
        return 0.0;
    }

    // 1. Get the class of the object (thisObj is the Student instance)
    jclass studentClass = env->GetObjectClass(thisObj);
    if (studentClass == nullptr) {
        return 0.0; // Class not found
    }

    // 2. Get the Field IDs for the three grade fields.
    // "D" is the JNI signature for the Java primitive 'double'.
    jfieldID mathField = env->GetFieldID(studentClass, "mathGrade", "D");
    jfieldID scienceField = env->GetFieldID(studentClass, "scienceGrade", "D");
    jfieldID litField = env->GetFieldID(studentClass, "literatureGrade", "D");

    // Check if any of the fields were not found
    if (mathField == nullptr || scienceField == nullptr || litField == nullptr) {
        return 0.0; 
    }

    // 3. Extract the double values from the Student object
    jdouble mathGrade = env->GetDoubleField(thisObj, mathField);
    jdouble scienceGrade = env->GetDoubleField(thisObj, scienceField);
    jdouble literatureGrade = env->GetDoubleField(thisObj, litField);

    // 4. Compute and return the average
    jdouble average = (mathGrade + scienceGrade + literatureGrade) / 3.0;

    return average;
}