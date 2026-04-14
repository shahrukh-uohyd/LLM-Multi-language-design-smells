#include <jni.h>
#include <stdlib.h>

#define SEVERITY_LEVELS_COUNT 4

JNIEXPORT jintArray JNICALL
Java_com_example_log_LogAnalyzer_computeSeverityStatistics(JNIEnv *env, jobject thiz, jobjectArray eventsArray) {
    
    // 1. Check for null input
    if (eventsArray == NULL) {
        return NULL;
    }

    // 2. Initialize a local C array to hold our counts
    jint counts[SEVERITY_LEVELS_COUNT] = {0, 0, 0, 0};

    jsize length = (*env)->GetArrayLength(env, eventsArray);

    if (length > 0) {
        // 3. Cache the Class and Field ID ONCE before looping (Performance Best Practice)
        jclass logEventClass = (*env)->FindClass(env, "com/example/log/LogEvent");
        if (logEventClass == NULL) {
            return NULL; // JNI throws NoClassDefFoundError automatically
        }

        jfieldID severityFieldId = (*env)->GetFieldID(env, logEventClass, "severityLevel", "I");
        if (severityFieldId == NULL) {
            (*env)->DeleteLocalRef(env, logEventClass);
            return NULL; // JNI throws NoSuchFieldError automatically
        }

        // 4. Iterate over the batch of logs
        for (jsize i = 0; i < length; i++) {
            jobject eventObj = (*env)->GetObjectArrayElement(env, eventsArray, i);
            
            if (eventObj != NULL) {
                // Extract the severity level integer
                jint severity = (*env)->GetIntField(env, eventObj, severityFieldId);
                
                // Increment the appropriate counter
                if (severity >= 0 && severity < SEVERITY_LEVELS_COUNT) {
                    counts[severity]++;
                }

                // 5. CRITICAL: Delete local reference to avoid table overflow!
                (*env)->DeleteLocalRef(env, eventObj);
            }
        }
        
        // Clean up class reference
        (*env)->DeleteLocalRef(env, logEventClass);
    }

    // 6. Create a new Java int array to return the results
    jintArray result = (*env)->NewIntArray(env, SEVERITY_LEVELS_COUNT);
    if (result != NULL) {
        // Copy the local C array 'counts' into the Java array 'result'
        (*env)->SetIntArrayRegion(env, result, 0, SEVERITY_LEVELS_COUNT, counts);
    }

    return result;
}