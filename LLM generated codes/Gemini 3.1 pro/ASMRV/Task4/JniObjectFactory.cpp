#include <jni.h>
#include <iostream>
#include <string>

extern "C" {

    JNIEXPORT jobject JNICALL Java_JniObjectFactory_createUserProfile(JNIEnv *env, jobject thisObj, jstring baseName, jint baseId) {
        
        // 1. Prepare data needed for object creation
        // Extract the base string from Java
        const char* cBaseName = env->GetStringUTFChars(baseName, nullptr);
        
        // Modify the data (e.g., append a suffix to the username, calculate new ID and status)
        std::string modifiedName = std::string(cBaseName) + "_Admin";
        jint finalId = baseId + 1000;
        jboolean isActive = (finalId > 1000) ? JNI_TRUE : JNI_FALSE;

        std::cout << "C++: Preparing data for object creation..." << std::endl;
        std::cout << "C++: Target Username: " << modifiedName << " | Target ID: " << finalId << std::endl;

        // Release the original string back to JNI
        env->ReleaseStringUTFChars(baseName, cBaseName);
        
        // Create a new JNI string for the constructor parameter
        jstring finalUsername = env->NewStringUTF(modifiedName.c_str());

        // 2. Locate the Java class
        jclass userProfileClass = env->FindClass("UserProfile");
        if (userProfileClass == nullptr) {
            std::cerr << "C++ Error: Could not find class 'UserProfile'." << std::endl;
            return nullptr;
        }

        // 3. Locate the constructor
        // Constructors are named "<init>". 
        // Signature "(Ljava/lang/String;IZ)V" maps to (String, int, boolean) returning void.
        jmethodID constructorID = env->GetMethodID(userProfileClass, "<init>", "(Ljava/lang/String;IZ)V");
        if (constructorID == nullptr) {
            std::cerr << "C++ Error: Could not find constructor." << std::endl;
            return nullptr;
        }

        // 4. Construct the object using the prepared data
        jobject newUserProfile = env->NewObject(userProfileClass, constructorID, finalUsername, finalId, isActive);

        std::cout << "C++: Object constructed successfully." << std::endl;

        // 5. Return the prepared result (the new object) to Java
        return newUserProfile;
    }

}