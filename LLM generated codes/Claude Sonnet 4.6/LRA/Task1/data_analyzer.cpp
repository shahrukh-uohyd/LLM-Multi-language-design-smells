#include <jni.h>
#include <stdexcept>

/**
 * JNI implementation for DataAnalyzer.sumUserRecordValues()
 *
 * Iterates through a jobjectArray of UserRecord objects,
 * retrieves the integer field 'value' from each, and returns
 * the accumulated sum as a jlong.
 *
 * Java signature: public native long sumUserRecordValues(UserRecord[] records);
 *
 * Method name convention: Java_<ClassName>_<MethodName>
 */
extern "C"
JNIEXPORT jlong JNICALL
Java_DataAnalyzer_sumUserRecordValues(JNIEnv* env, jobject /* thiz */, jobjectArray records) {

    // -------------------------------------------------------------------------
    // Step 1: Validate input array
    // -------------------------------------------------------------------------
    if (records == nullptr) {
        jclass illegalArgEx = env->FindClass("java/lang/IllegalArgumentException");
        if (illegalArgEx != nullptr) {
            env->ThrowNew(illegalArgEx, "records array must not be null");
        }
        return 0L;
    }

    // -------------------------------------------------------------------------
    // Step 2: Get the array length
    // -------------------------------------------------------------------------
    jsize arrayLength = env->GetArrayLength(records);
    if (arrayLength == 0) {
        return 0L; // Nothing to sum
    }

    // -------------------------------------------------------------------------
    // Step 3: Retrieve the UserRecord class reference
    //
    // We grab the first element to resolve the class dynamically.
    // This avoids hardcoding the full class path if it's in a package.
    //
    // Alternative (if class path is known):
    //   jclass userRecordClass = env->FindClass("com/example/UserRecord");
    // -------------------------------------------------------------------------
    jobject firstElement = env->GetObjectArrayElement(records, 0);
    if (firstElement == nullptr) {
        // First element is null — fall back to FindClass
        // Adjust the path to match your actual package, e.g. "com/example/UserRecord"
        jclass userRecordClass = env->FindClass("UserRecord");
        if (userRecordClass == nullptr) {
            // FindClass itself throws NoClassDefFoundError; propagate it
            return 0L;
        }
        env->DeleteLocalRef(userRecordClass);
        return 0L;
    }

    jclass userRecordClass = env->GetObjectClass(firstElement);
    env->DeleteLocalRef(firstElement); // Release the reference; we only needed the class

    if (userRecordClass == nullptr) {
        jclass runtimeEx = env->FindClass("java/lang/RuntimeException");
        if (runtimeEx != nullptr) {
            env->ThrowNew(runtimeEx, "Failed to retrieve UserRecord class");
        }
        return 0L;
    }

    // -------------------------------------------------------------------------
    // Step 4: Get the field ID for the 'value' integer field
    //
    // Signature "I" corresponds to Java's primitive int type.
    // If the field is named differently in your class, update "value" here.
    // -------------------------------------------------------------------------
    jfieldID valueFieldId = env->GetFieldID(userRecordClass, "value", "I");
    if (valueFieldId == nullptr) {
        // GetFieldID throws NoSuchFieldError automatically; just return
        env->DeleteLocalRef(userRecordClass);
        return 0L;
    }

    // -------------------------------------------------------------------------
    // Step 5: Iterate through all elements and accumulate the sum
    // -------------------------------------------------------------------------
    jlong sum = 0L;

    for (jsize i = 0; i < arrayLength; ++i) {
        jobject record = env->GetObjectArrayElement(records, i);

        if (record == nullptr) {
            // Gracefully skip null elements — log if needed
            continue;
        }

        // Extract the integer field from the current UserRecord object
        jint fieldValue = env->GetIntField(record, valueFieldId);

        // Check for pending JVM exceptions after each field access (optional but safe)
        if (env->ExceptionCheck()) {
            env->DeleteLocalRef(record);
            env->DeleteLocalRef(userRecordClass);
            return 0L; // Propagate exception back to Java
        }

        sum += static_cast<jlong>(fieldValue);

        // !! CRITICAL: Release local reference to avoid local ref table overflow
        //    The JNI local reference table typically holds ~512 entries.
        //    With 100 elements this is fine, but best practice is to always
        //    delete local refs inside loops.
        env->DeleteLocalRef(record);
    }

    // -------------------------------------------------------------------------
    // Step 6: Clean up class reference and return result
    // -------------------------------------------------------------------------
    env->DeleteLocalRef(userRecordClass);

    return sum;
}