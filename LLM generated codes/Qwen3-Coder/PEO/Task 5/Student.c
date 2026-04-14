#include <jni.h>
#include "Student.h"

/*
 * Class:     Student
 * Method:    computeAverageGrade
 * Signature: ()D
 */
JNIEXPORT jdouble JNICALL Java_Student_computeAverageGrade
  (JNIEnv *env, jobject thisObj) {
    
    // Get the Student class
    jclass studentClass = (*env)->GetObjectClass(env, thisObj);
    
    // Find the field IDs for the grade fields
    jfieldID mathGradeFieldID = (*env)->GetFieldID(env, studentClass, "mathGrade", "D");
    jfieldID scienceGradeFieldID = (*env)->GetFieldID(env, studentClass, "scienceGrade", "D");
    jfieldID literatureGradeFieldID = (*env)->GetFieldID(env, studentClass, "literatureGrade", "D");
    
    if (!mathGradeFieldID || !scienceGradeFieldID || !literatureGradeFieldID) {
        // One or more fields not found
        (*env)->ExceptionDescribe(env);
        return 0.0;  // Return 0.0 as default average
    }
    
    // Get the values of the grade fields
    jdouble mathGrade = (*env)->GetDoubleField(env, thisObj, mathGradeFieldID);
    jdouble scienceGrade = (*env)->GetDoubleField(env, thisObj, scienceGradeFieldID);
    jdouble literatureGrade = (*env)->GetDoubleField(env, thisObj, literatureGradeFieldID);
    
    // Compute the average grade
    jdouble averageGrade = (mathGrade + scienceGrade + literatureGrade) / 3.0;
    
    return averageGrade;
}

/*
 * Class:     Student
 * Method:    computeWeightedAverage
 * Signature: (DDD)D
 */
JNIEXPORT jdouble JNICALL Java_Student_computeWeightedAverage
  (JNIEnv *env, jobject thisObj, jdouble mathWeight, jdouble scienceWeight, jdouble literatureWeight) {
    
    // Get the Student class
    jclass studentClass = (*env)->GetObjectClass(env, thisObj);
    
    // Find the field IDs for the grade fields
    jfieldID mathGradeFieldID = (*env)->GetFieldID(env, studentClass, "mathGrade", "D");
    jfieldID scienceGradeFieldID = (*env)->GetFieldID(env, studentClass, "scienceGrade", "D");
    jfieldID literatureGradeFieldID = (*env)->GetFieldID(env, studentClass, "literatureGrade", "D");
    
    if (!mathGradeFieldID || !scienceGradeFieldID || !literatureGradeFieldID) {
        (*env)->ExceptionDescribe(env);
        return 0.0;
    }
    
    // Get the values of the grade fields
    jdouble mathGrade = (*env)->GetDoubleField(env, thisObj, mathGradeFieldID);
    jdouble scienceGrade = (*env)->GetDoubleField(env, thisObj, scienceGradeFieldID);
    jdouble literatureGrade = (*env)->GetDoubleField(env, thisObj, literatureGradeFieldID);
    
    // Validate weights are non-negative
    if (mathWeight < 0.0 || scienceWeight < 0.0 || literatureWeight < 0.0) {
        (*env)->ThrowNew(env, (*env)->FindClass(env, "IllegalArgumentException"), 
                       "Weights must be non-negative");
        return 0.0;
    }
    
    // Calculate total weight
    jdouble totalWeight = mathWeight + scienceWeight + literatureWeight;
    
    if (totalWeight == 0.0) {
        // If all weights are 0, fall back to simple average
        return (mathGrade + scienceGrade + literatureGrade) / 3.0;
    }
    
    // Compute weighted average
    jdouble weightedAverage = (mathGrade * mathWeight + scienceGrade * scienceWeight + literatureGrade * literatureWeight) / totalWeight;
    
    return weightedAverage;
}