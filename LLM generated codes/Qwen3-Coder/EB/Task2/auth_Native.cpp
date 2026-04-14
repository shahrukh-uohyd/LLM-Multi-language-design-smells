#include <jni.h>
#include <string>
#include <iostream>
#include <sstream>
#include <iomanip>
#include <ctime>
#include <algorithm>
#include <cctype>
#include <openssl/sha.h>  // You may need to install OpenSSL
#include <openssl/aes.h>  // For encryption
#include <random>
#include "UserAuthenticator.h"

// Helper function to get field IDs
static jfieldID getUsernameFieldID(JNIEnv* env, jobject obj) {
    jclass clazz = env->GetObjectClass(obj);
    return env->GetFieldID(clazz, "username", "Ljava/lang/String;");
}

static jfieldID getPasswordHashFieldID(JNIEnv* env, jobject obj) {
    jclass clazz = env->GetObjectClass(obj);
    return env->GetFieldID(clazz, "passwordHash", "Ljava/lang/String;");
}

static jfieldID getLoginAttemptsFieldID(JNIEnv* env, jobject obj) {
    jclass clazz = env->GetObjectClass(obj);
    return env->GetFieldID(clazz, "loginAttempts", "I");
}

static jfieldID getIsAuthenticatedFieldID(JNIEnv* env, jobject obj) {
    jclass clazz = env->GetObjectClass(obj);
    return env->GetFieldID(clazz, "isAuthenticated", "Z");
}

static jfieldID getRoleFieldID(JNIEnv* env, jobject obj) {
    jclass clazz = env->GetObjectClass(obj);
    return env->GetFieldID(clazz, "role", "Ljava/lang/String;");
}

// Simple encryption function (in practice, use proper encryption libraries)
std::string encryptSimple(const std::string& input) {
    std::string result = input;
    for (char& c : result) {
        c = c ^ 0x5A; // XOR with key
    }
    return result;
}

// Simple decryption function
std::string decryptSimple(const std::string& input) {
    std::string result = input;
    for (char& c : result) {
        c = c ^ 0x5A; // XOR with same key
    }
    return result;
}

// Generate random session token
std::string generateRandomToken() {
    std::random_device rd;
    std::mt19937 gen(rd());
    std::uniform_int_distribution<> dis(0, 15);
    
    std::stringstream ss;
    ss << std::hex << std::uppercase;
    
    for (int i = 0; i < 32; ++i) {
        ss << dis(gen);
    }
    
    return ss.str();
}

// Check password strength
bool isPasswordStrong(const std::string& password) {
    if (password.length() < 8) return false;
    
    bool hasUpper = false, hasLower = false, hasDigit = false, hasSpecial = false;
    
    for (char c : password) {
        if (std::isupper(c)) hasUpper = true;
        else if (std::islower(c)) hasLower = true;
        else if (std::isdigit(c)) hasDigit = true;
        else if (std::ispunct(c)) hasSpecial = true;
    }
    
    return hasUpper && hasLower && hasDigit && hasSpecial;
}

/*
 * Class:     UserAuthenticator
 * Method:    authenticateWithNative
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_UserAuthenticator_authenticateWithNative
  (JNIEnv *env, jobject obj, jstring inputPassword) {
    
    if (inputPassword == nullptr) {
        std::cout << "[NATIVE AUTH] Input password is null" << std::endl;
        return JNI_FALSE;
    }

    // Get stored password hash from Java object
    jfieldID passwordHashField = getPasswordHashFieldID(env, obj);
    jstring storedHash = (jstring)env->GetObjectField(obj, passwordHashField);
    
    if (storedHash == nullptr) {
        std::cout << "[NATIVE AUTH] Stored password hash is null" << std::endl;
        return JNI_FALSE;
    }

    // Convert jstrings to C++ strings
    const char* inputPasswordStr = env->GetStringUTFChars(inputPassword, nullptr);
    const char* storedHashStr = env->GetStringUTFChars(storedHash, nullptr);
    
    if (inputPasswordStr == nullptr || storedHashStr == nullptr) {
        std::cout << "[NATIVE AUTH] Failed to convert strings" << std::endl;
        if (inputPasswordStr) env->ReleaseStringUTFChars(inputPassword, inputPasswordStr);
        if (storedHashStr) env->ReleaseStringUTFChars(storedHash, storedHashStr);
        return JNI_FALSE;
    }

    // Simple hash comparison (in practice, use proper secure comparison)
    std::string inputHash = std::to_string(std::hash<std::string>{}(inputPasswordStr));
    bool match = (inputHash == storedHashStr);
    
    std::cout << "[NATIVE AUTH] Authentication attempt for user. Match: " << (match ? "SUCCESS" : "FAILURE") << std::endl;
    
    // Release string resources
    env->ReleaseStringUTFChars(inputPassword, inputPasswordStr);
    env->ReleaseStringUTFChars(storedHash, storedHashStr);
    
    // Clean up local references
    env->DeleteLocalRef(storedHash);
    
    return match ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     UserAuthenticator
 * Method:    encryptCredentials
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_UserAuthenticator_encryptCredentials
  (JNIEnv *env, jobject obj, jstring credentials) {
    
    if (credentials == nullptr) {
        return nullptr;
    }

    const char* credsStr = env->GetStringUTFChars(credentials, nullptr);
    if (credsStr == nullptr) {
        return nullptr;
    }

    std::string plainText(credsStr);
    std::string encrypted = encryptSimple(plainText);
    
    std::cout << "[NATIVE AUTH] Encrypted credentials: " << plainText << " -> [ENCRYPTED]" << std::endl;
    
    env->ReleaseStringUTFChars(credentials, credsStr);
    
    return env->NewStringUTF(encrypted.c_str());
}

/*
 * Class:     UserAuthenticator
 * Method:    validateUserSecurity
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_UserAuthenticator_validateUserSecurity
  (JNIEnv *env, jobject obj) {
    
    // Get user information from Java object
    jfieldID usernameField = getUsernameFieldID(env, obj);
    jfieldID loginAttemptsField = getLoginAttemptsFieldID(env, obj);
    jfieldID isAuthenticatedField = getIsAuthenticatedFieldID(env, obj);
    jfieldID roleField = getRoleFieldID(env, obj);
    
    jstring username = (jstring)env->GetObjectField(obj, usernameField);
    jint loginAttempts = env->GetIntField(obj, loginAttemptsField);
    jboolean isAuthenticated = env->GetBooleanField(obj, isAuthenticatedField);
    jstring role = (jstring)env->GetObjectField(obj, roleField);
    
    const char* usernameStr = env->GetStringUTFChars(username, nullptr);
    const char* roleStr = env->GetStringUTFChars(role, nullptr);
    
    bool isValid = true;
    
    // Security checks
    if (loginAttempts > 5) {
        std::cout << "[NATIVE AUTH] Security validation failed: Too many login attempts (" << loginAttempts << ")" << std::endl;
        isValid = false;
    }
    
    if (usernameStr && strlen(usernameStr) < 3) {
        std::cout << "[NATIVE AUTH] Security validation failed: Username too short" << std::endl;
        isValid = false;
    }
    
    if (roleStr && (strcmp(roleStr, "ADMIN") != 0 && strcmp(roleStr, "USER") != 0)) {
        std::cout << "[NATIVE AUTH] Security validation failed: Invalid role" << std::endl;
        isValid = false;
    }
    
    // Additional security check: authenticated users have different validation rules
    if (isAuthenticated && loginAttempts > 0) {
        std::cout << "[NATIVE AUTH] Warning: Authenticated user has login attempts recorded" << std::endl;
    }
    
    // Cleanup
    if (usernameStr) env->ReleaseStringUTFChars(username, usernameStr);
    if (roleStr) env->ReleaseStringUTFChars(role, roleStr);
    
    env->DeleteLocalRef(username);
    env->DeleteLocalRef(role);
    
    std::cout << "[NATIVE AUTH] Security validation result: " << (isValid ? "PASSED" : "FAILED") << std::endl;
    
    return isValid ? JNI_TRUE : JNI_FALSE;
}

/*
 * Class:     UserAuthenticator
 * Method:    generateSessionToken
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_UserAuthenticator_generateSessionToken
  (JNIEnv *env, jobject obj) {
    
    // Get username to include in token generation context
    jfieldID usernameField = getUsernameFieldID(env, obj);
    jstring username = (jstring)env->GetObjectField(obj, usernameField);
    
    const char* usernameStr = env->GetStringUTFChars(username, nullptr);
    
    // Generate token based on username and timestamp
    std::string token = generateRandomToken();
    
    // In a real system, you might want to store this token somewhere
    std::time_t now = std::time(nullptr);
    std::cout << "[NATIVE AUTH] Generated session token for user '" << usernameStr 
              << "' at " << std::asctime(std::localtime(&now)) << ": " << token << std::endl;
    
    env->ReleaseStringUTFChars(username, usernameStr);
    env->DeleteLocalRef(username);
    
    return env->NewStringUTF(token.c_str());
}

/*
 * Class:     UserAuthenticator
 * Method:    logAuthenticationEvent
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_UserAuthenticator_logAuthenticationEvent
  (JNIEnv *env, jobject obj, jstring eventType) {
    
    const char* eventTypeStr = env->GetStringUTFChars(eventType, nullptr);
    if (eventTypeStr == nullptr) return;

    // Get user info for logging
    jfieldID usernameField = getUsernameFieldID(env, obj);
    jfieldID loginAttemptsField = getLoginAttemptsFieldID(env, obj);
    jfieldID isAuthenticatedField = getIsAuthenticatedFieldID(env, obj);
    
    jstring username = (jstring)env->GetObjectField(obj, usernameField);
    jint loginAttempts = env->GetIntField(obj, loginAttemptsField);
    jboolean isAuthenticated = env->GetBooleanField(obj, isAuthenticatedField);
    
    const char* usernameStr = env->GetStringUTFChars(username, nullptr);
    
    std::time_t now = std::time(nullptr);
    std::cout << "[NATIVE AUTH LOG] [" << std::asctime(std::localtime(&now)) 
              << "] User: " << usernameStr 
              << ", Event: " << eventTypeStr 
              << ", Login Attempts: " << loginAttempts 
              << ", Authenticated: " << (isAuthenticated ? "YES" : "NO") << std::endl;
    
    // Here you would typically write to a secure log file or database
    // For demo purposes, we just print to console
    
    env->ReleaseStringUTFChars(eventType, eventTypeStr);
    env->ReleaseStringUTFChars(username, usernameStr);
    
    env->DeleteLocalRef(username);
}

/*
 * Class:     UserAuthenticator
 * Method:    checkPasswordStrength
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_UserAuthenticator_checkPasswordStrength
  (JNIEnv *env, jobject obj, jstring password) {
    
    if (password == nullptr) {
        return JNI_FALSE;
    }

    const char* passwordStr = env->GetStringUTFChars(password, nullptr);
    if (passwordStr == nullptr) {
        return JNI_FALSE;
    }

    bool isStrong = isPasswordStrength(passwordStr);
    
    std::cout << "[NATIVE AUTH] Password strength check for '" << passwordStr 
              << "': " << (isStrong ? "STRONG" : "WEAK") << std::endl;
    
    env->ReleaseStringUTFChars(password, passwordStr);
    
    return isStrong ? JNI_TRUE : JNI_FALSE;
}