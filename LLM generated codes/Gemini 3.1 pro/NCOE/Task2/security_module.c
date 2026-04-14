#include <jni.h>
#include <stddef.h>

JNIEXPORT void JNICALL Java_com_example_security_SecurityModule_sanitizeProfile(
    JNIEnv *env, 
    jobject thiz,       // The SecurityModule instance
    jobject profile)    // The UserProfile instance passed as an argument
{
    // 1. Null check for safety
    if (profile == NULL) {
        return;
    }

    // 2. Get the class of the UserProfile object
    jclass profileClass = (*env)->GetObjectClass(env, profile);
    if (profileClass == NULL) {
        return;
    }

    // 3. Look up the Field ID for the "bio" String.
    // "Ljava/lang/String;" is the internal JNI signature for java.lang.String
    jfieldID bioFieldId = (*env)->GetFieldID(env, profileClass, "bio", "Ljava/lang/String;");
    if (bioFieldId == NULL) {
        (*env)->DeleteLocalRef(env, profileClass);
        return; // Field not found
    }

    // 4. Look up the actual String object from the field
    jstring bioStr = (jstring) (*env)->GetObjectField(env, profile, bioFieldId);
    if (bioStr == NULL) {
        (*env)->DeleteLocalRef(env, profileClass);
        return; // bio is null, nothing to truncate
    }

    // 5. Check the length of the string (Returns the number of UTF-16 characters)
    jsize length = (*env)->GetStringLength(env, bioStr);

    // 6. If the length exceeds 100 characters, truncate and set a new value
    if (length > 100) {
        // Retrieve the raw UTF-16 characters from the Java string
        const jchar *chars = (*env)->GetStringChars(env, bioStr, NULL);
        if (chars != NULL) {
            
            // Create a brand new Java String using only the first 100 characters
            jstring truncatedBioStr = (*env)->NewString(env, chars, 100);

            // Always release the string chars when done to avoid memory leaks
            (*env)->ReleaseStringChars(env, bioStr, chars);

            if (truncatedBioStr != NULL) {
                // Look up that same bio field again (using our cached bioFieldId) 
                // and set it to the new truncated string.
                (*env)->SetObjectField(env, profile, bioFieldId, truncatedBioStr);
                
                // Clean up the local reference for the new string
                (*env)->DeleteLocalRef(env, truncatedBioStr);
            }
        }
    }

    // 7. Clean up remaining local references
    (*env)->DeleteLocalRef(env, bioStr);
    (*env)->DeleteLocalRef(env, profileClass);
}