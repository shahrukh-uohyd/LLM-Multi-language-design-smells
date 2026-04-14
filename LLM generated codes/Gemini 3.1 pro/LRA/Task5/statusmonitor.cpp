#include <jni.h>
#include <string>
#include <unordered_map>
#include <vector>

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_example_monitoring_StatusMonitor_findRepeatedFailures(JNIEnv *env, jobject thiz, jobjectArray updatesArray, jint failureCode, jint threshold) {
    
    if (updatesArray == nullptr) return nullptr;

    jsize length = env->GetArrayLength(updatesArray);
    if (length == 0) return nullptr;

    // 1. Look up the DeviceStatusUpdate class
    jclass updateClass = env->FindClass("com/example/monitoring/DeviceStatusUpdate");
    if (updateClass == nullptr) return nullptr;

    // 2. Cache Field IDs
    // "I" is for int, "Ljava/lang/String;" is the signature for a Java String object.
    jfieldID statusCodeFieldId = env->GetFieldID(updateClass, "statusCode", "I");
    jfieldID deviceIdFieldId = env->GetFieldID(updateClass, "deviceId", "Ljava/lang/String;");

    if (statusCodeFieldId == nullptr || deviceIdFieldId == nullptr) {
        env->DeleteLocalRef(updateClass);
        return nullptr;
    }

    // Hash map to track failure occurrences: Key = Device ID, Value = Failure Count
    std::unordered_map<std::string, int> failureCounts;

    // 3. Iterate through the updates
    for (jsize i = 0; i < length; ++i) {
        jobject updateObj = env->GetObjectArrayElement(updatesArray, i);
        
        if (updateObj != nullptr) {
            // Read the integer status code
            jint status = env->GetIntField(updateObj, statusCodeFieldId);

            // Only process the String ID if it's a failure (Performance Optimization)
            if (status == failureCode) {
                jstring deviceIdJStr = (jstring) env->GetObjectField(updateObj, deviceIdFieldId);
                
                if (deviceIdJStr != nullptr) {
                    // Convert Java String to C++ std::string
                    const char *deviceIdCStr = env->GetStringUTFChars(deviceIdJStr, nullptr);
                    if (deviceIdCStr != nullptr) {
                        
                        // Increment the count in our C++ unordered_map
                        failureCounts[std::string(deviceIdCStr)]++;
                        
                        // Always release the C string when done
                        env->ReleaseStringUTFChars(deviceIdJStr, deviceIdCStr);
                    }
                    
                    // CRITICAL: Delete local ref to the nested string object
                    env->DeleteLocalRef(deviceIdJStr);
                }
            }
            
            // CRITICAL: Delete local ref to the array element
            env->DeleteLocalRef(updateObj);
        }
    }

    // Clean up class ref as we are done reading objects
    env->DeleteLocalRef(updateClass);

    // 4. Identify devices meeting the threshold
    std::vector<std::string> flaggedDevices;
    for (const auto& pair : failureCounts) {
        if (pair.second >= threshold) {
            flaggedDevices.push_back(pair.first);
        }
    }

    // 5. Construct the result array (String[])
    jclass stringClass = env->FindClass("java/lang/String");
    if (stringClass == nullptr) return nullptr;

    jobjectArray resultArray = env->NewObjectArray(flaggedDevices.size(), stringClass, nullptr);
    if (resultArray == nullptr) {
        env->DeleteLocalRef(stringClass);
        return nullptr;
    }

    // 6. Populate the result array
    for (size_t i = 0; i < flaggedDevices.size(); ++i) {
        // Convert C++ std::string back to Java String
        jstring newStr = env->NewStringUTF(flaggedDevices[i].c_str());
        
        if (newStr != nullptr) {
            env->SetObjectArrayElement(resultArray, i, newStr);
            // Delete local ref of the new string after assigning it to the array
            env->DeleteLocalRef(newStr);
        }
    }

    env->DeleteLocalRef(stringClass);

    return resultArray;
}