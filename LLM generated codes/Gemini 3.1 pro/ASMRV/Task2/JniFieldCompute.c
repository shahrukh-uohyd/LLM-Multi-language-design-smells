#include <jni.h>
#include <stdio.h>

// JNI Implementation for the computeTotalValue method
JNIEXPORT jdouble JNICALL Java_JniFieldCompute_computeTotalValue(JNIEnv *env, jobject thisObj, jobject itemObj) {
    if (itemObj == NULL) {
        return 0.0;
    }

    // 1. Get the class of the passed Java object
    jclass itemClass = (*env)->GetObjectClass(env, itemObj);

    // 2. Identify the fields of the class using their names and JNI signatures
    // "I" stands for int, "D" stands for double
    jfieldID fidQuantity = (*env)->GetFieldID(env, itemClass, "quantity", "I");
    jfieldID fidPrice = (*env)->GetFieldID(env, itemClass, "price", "D");

    if (fidQuantity == NULL || fidPrice == NULL) {
        printf("Error: Could not locate fields in the Java class.\n");
        return -1.0;
    }

    // 3. Extract the actual values of the fields from the object
    jint quantity = (*env)->GetIntField(env, itemObj, fidQuantity);
    jdouble price = (*env)->GetDoubleField(env, itemObj, fidPrice);

    // 4. Compute a value derived from the fields (Total Value = quantity * price)
    jdouble computedValue = (jdouble)(quantity * price);

    // 5. Return the computed value to Java
    return computedValue;
}