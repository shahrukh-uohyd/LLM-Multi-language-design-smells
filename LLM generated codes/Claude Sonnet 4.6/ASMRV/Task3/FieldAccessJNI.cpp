/*
 * FieldAccessJNI.cpp
 *
 * C++ implementation of:
 *   public native int getQuantity(Product product);
 *
 * Because there is no package, the JNI mangled name is simply:
 *   Java_FieldAccessJNI_getQuantity
 *
 * Steps:
 *   1. Receive the Java Product object as a jobject parameter.
 *   2. Obtain its jclass via GetObjectClass().
 *   3. Resolve the 'quantity' field ID via GetFieldID() with descriptor "I".
 *   4. Read the integer value via GetIntField().
 *   5. Return the jint to Java.
 */

#include <jni.h>

// ---------------------------------------------------------------------------
// Helper: throw a named Java exception from native code.
// ---------------------------------------------------------------------------
static void throwJavaException(JNIEnv   *env,
                                const char *exceptionClass,
                                const char *message)
{
    jclass cls = env->FindClass(exceptionClass);
    if (cls != nullptr) {
        env->ThrowNew(cls, message);
        env->DeleteLocalRef(cls);
    }
}

// ---------------------------------------------------------------------------
// extern "C" prevents C++ name mangling so the JVM can find the symbol.
// ---------------------------------------------------------------------------
extern "C" {

/*
 * Class:     FieldAccessJNI          (no package prefix)
 * Method:    getQuantity
 * Signature: (LProduct;)I
 *
 * env        – JNI function table pointer
 * thisObj    – the FieldAccessJNI instance (unused here)
 * productObj – the Product instance passed from Java
 */
JNIEXPORT jint JNICALL
Java_FieldAccessJNI_getQuantity(JNIEnv *env,
                                jobject thisObj,
                                jobject productObj)
{
    // ------------------------------------------------------------------
    // Guard: reject a null object immediately.
    // ------------------------------------------------------------------
    if (productObj == nullptr) {
        throwJavaException(env,
            "java/lang/IllegalArgumentException",
            "getQuantity: product must not be null");
        return -1;
    }

    // ------------------------------------------------------------------
    // Step 1 – Get the jclass of the incoming object.
    //
    // GetObjectClass() derives the class directly from the live object.
    // It never returns null for a non-null jobject.
    // ------------------------------------------------------------------
    jclass productClass = env->GetObjectClass(productObj);

    if (productClass == nullptr) {
        throwJavaException(env,
            "java/lang/RuntimeException",
            "getQuantity: GetObjectClass returned null");
        return -1;
    }

    // ------------------------------------------------------------------
    // Step 2 – Resolve the field ID for 'quantity'.
    //
    // GetFieldID() searches the class hierarchy for an instance field
    // matching the given name and JVM type descriptor.
    //
    // Type descriptor for 'int' is "I".
    // Other common descriptors:
    //   "J" → long     "D" → double    "F" → float
    //   "Z" → boolean  "B" → byte      "C" → char    "S" → short
    //   "Ljava/lang/String;" → String
    //
    // Returns null and throws NoSuchFieldError if not found.
    // ------------------------------------------------------------------
    jfieldID quantityFieldID = env->GetFieldID(productClass,  // class to search
                                               "quantity",     // field name
                                               "I");           // type descriptor: int

    if (quantityFieldID == nullptr) {
        // NoSuchFieldError is already pending; just clean up.
        env->DeleteLocalRef(productClass);
        return -1;
    }

    // ------------------------------------------------------------------
    // Step 3 – Read the integer value of the field from the object.
    //
    // GetIntField() reads the 'int' field identified by quantityFieldID
    // directly from the productObj instance.
    // ------------------------------------------------------------------
    jint quantityValue = env->GetIntField(productObj, quantityFieldID);

    // ------------------------------------------------------------------
    // Step 4 – Release the local class reference (best practice).
    // ------------------------------------------------------------------
    env->DeleteLocalRef(productClass);

    // ------------------------------------------------------------------
    // Step 5 – Return the retrieved integer value to Java.
    // ------------------------------------------------------------------
    return quantityValue;
}

} // extern "C"