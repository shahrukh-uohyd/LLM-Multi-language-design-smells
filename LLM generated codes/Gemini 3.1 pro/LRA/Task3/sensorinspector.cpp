#include <jni.h>
#include <vector>

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_example_monitoring_ThresholdInspector_findExceedingReadings(JNIEnv *env, jobject thiz, jobjectArray readingsArray, jdouble threshold) {
    
    if (readingsArray == nullptr) {
        return nullptr;
    }

    jsize length = env->GetArrayLength(readingsArray);
    if (length == 0) {
        return nullptr;
    }

    // 1. Cache the class and field ID
    jclass readingClass = env->FindClass("com/example/monitoring/SensorReading");
    if (readingClass == nullptr) {
        return nullptr; // JVM throws NoClassDefFoundError
    }

    // "D" is the JNI signature for the Java primitive 'double'
    jfieldID valueFieldId = env->GetFieldID(readingClass, "measurementValue", "D");
    if (valueFieldId == nullptr) {
        env->DeleteLocalRef(readingClass);
        return nullptr; // JVM throws NoSuchFieldError
    }

    // 2. Use a C++ vector to store the INDICES of exceeding elements.
    // We store indices instead of jobjects to prevent Local Reference Table Overflow.
    std::vector<jsize> exceedingIndices;

    for (jsize i = 0; i < length; ++i) {
        jobject readingObj = env->GetObjectArrayElement(readingsArray, i);
        if (readingObj != nullptr) {
            
            jdouble value = env->GetDoubleField(readingObj, valueFieldId);
            
            // Check against our threshold
            if (value > threshold) {
                exceedingIndices.push_back(i);
            }
            
            // Delete reference immediately after inspection
            env->DeleteLocalRef(readingObj);
        }
    }

    // 3. Create a new Java Object Array of the exact size needed
    jobjectArray resultArray = env->NewObjectArray(exceedingIndices.size(), readingClass, nullptr);
    if (resultArray == nullptr) {
        // OutOfMemoryError is thrown by JVM if array creation fails
        env->DeleteLocalRef(readingClass);
        return nullptr;
    }

    // 4. Populate the result array using the saved indices
    for (size_t i = 0; i < exceedingIndices.size(); ++i) {
        jsize originalIndex = exceedingIndices[i];
        
        // Re-fetch only the required object
        jobject exceedingObj = env->GetObjectArrayElement(readingsArray, originalIndex);
        
        // Place it into the new array
        env->SetObjectArrayElement(resultArray, i, exceedingObj);
        
        // Clean up
        env->DeleteLocalRef(exceedingObj);
    }

    // Free the class reference
    env->DeleteLocalRef(readingClass);

    return resultArray;
}