#include <jni.h>
#include <stdio.h>

/*
 * Implements: public native String getPersonName(Person person);
 *
 * JNI naming convention (no package, so no underscores from package path):
 *   Java_<ClassName>_<methodName>
 */
JNIEXPORT jstring JNICALL
Java_NativeBridge_getPersonName(JNIEnv *env, jobject thisObj, jobject personObj)
{
    /* Step 1: Get the Class of the passed Person object */
    jclass personClass = (*env)->GetObjectClass(env, personObj);
    if (personClass == NULL) {
        fprintf(stderr, "[native] ERROR: Could not get Person class.\n");
        return NULL;
    }

    /* Step 2: Look up the field ID for "name" (type signature: Ljava/lang/String;) */
    jfieldID nameFieldID = (*env)->GetFieldID(env, personClass, "name", "Ljava/lang/String;");
    if (nameFieldID == NULL) {
        fprintf(stderr, "[native] ERROR: Could not find field 'name' in Person.\n");
        return NULL;
    }

    /* Step 3: Read the field value as a jstring */
    jstring nameValue = (jstring)(*env)->GetObjectField(env, personObj, nameFieldID);
    if (nameValue == NULL) {
        fprintf(stderr, "[native] ERROR: Field 'name' returned NULL.\n");
        return NULL;
    }

    /* Step 4: Convert to C string just for native-side printing */
    const char *cName = (*env)->GetStringUTFChars(env, nameValue, NULL);
    if (cName != NULL) {
        printf("[native] Read Person.name = \"%s\"\n", cName);
        (*env)->ReleaseStringUTFChars(env, nameValue, cName); /* always release */
    }

    /* Step 5: Return the jstring back to Java */
    return nameValue;
}