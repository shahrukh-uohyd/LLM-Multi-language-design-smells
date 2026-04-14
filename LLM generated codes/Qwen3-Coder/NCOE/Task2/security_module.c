// security_module.c
#include <jni.h>
#include <string.h>
#include <stdlib.h>

// Maximum allowed length for bio
#define MAX_BIO_LENGTH 100

JNIEXPORT void JNICALL Java_UserProfile_sanitizeBioNative(JNIEnv *env, jobject userProfileObj) {
    // Get the class of the user profile object
    jclass userProfileClass = (*env)->GetObjectClass(env, userProfileObj);
    
    // Look up the bio field
    jfieldID bioFieldID = (*env)->GetFieldID(env, userProfileClass, "bio", "Ljava/lang/String;");
    
    // Check if field exists
    if (bioFieldID == NULL) {
        // Field not found - throw exception
        jclass exceptionClass = (*env)->FindClass(env, "java/lang/RuntimeException");
        (*env)->ThrowNew(env, exceptionClass, "Could not find 'bio' field in UserProfile class");
        return;
    }
    
    // Get the current bio string
    jstring bioString = (jstring)(*env)->GetObjectField(env, userProfileObj, bioFieldID);
    
    if (bioString == NULL) {
        // Bio is null, nothing to sanitize
        return;
    }
    
    // Get the C string from the Java string
    const char *bioCString = (*env)->GetStringUTFChars(env, bioString, NULL);
    
    if (bioCString == NULL) {
        // Out of memory error
        return;
    }
    
    // Check the length of the bio
    jsize bioLength = (*env)->GetStringUTFLength(env, bioString);
    
    if (bioLength > MAX_BIO_LENGTH) {
        // Need to truncate the bio
        char *truncatedBio = malloc(MAX_BIO_LENGTH + 1); // +1 for null terminator
        
        if (truncatedBio != NULL) {
            // Copy only the first MAX_BIO_LENGTH characters
            strncpy(truncatedBio, bioCString, MAX_BIO_LENGTH);
            truncatedBio[MAX_BIO_LENGTH] = '\0'; // Ensure null termination
            
            // Create a new Java string with the truncated bio
            jstring newBioString = (*env)->NewStringUTF(env, truncatedBio);
            
            if (newBioString != NULL) {
                // Set the new bio value back to the field
                // Look up the bio field again as required by the specification
                jfieldID newBioFieldID = (*env)->GetFieldID(env, userProfileClass, "bio", "Ljava/lang/String;");
                
                if (newBioFieldID != NULL) {
                    (*env)->SetObjectField(env, userProfileObj, newBioFieldID, newBioString);
                }
            }
            
            free(truncatedBio);
        }
    }
    
    // Release the C string reference
    (*env)->ReleaseStringUTFChars(env, bioString, bioCString);
}