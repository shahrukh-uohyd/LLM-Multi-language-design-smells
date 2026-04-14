#include <jni.h>
#include <string.h>
#include <stdlib.h>

static int extract_value(const char* config, const char* key) {
    const char* pos = strstr(config, key);
    if (!pos) return 0;

    pos += strlen(key);
    if (*pos != '=') return 0;
    pos++;

    return atoi(pos);
}

JNIEXPORT jintArray JNICALL
Java_NativeConfigParser_parseConfig(
        JNIEnv* env,
        jclass,
        jstring configStr,
        jobjectArray keysArray) {

    if (configStr == NULL || keysArray == NULL) {
        return NULL;
    }

    const char* config =
        (*env)->GetStringUTFChars(env, configStr, NULL);
    if (config == NULL) {
        return NULL;
    }

    jsize keyCount =
        (*env)->GetArrayLength(env, keysArray);

    jintArray result =
        (*env)->NewIntArray(env, keyCount);
    if (result == NULL) {
        (*env)->ReleaseStringUTFChars(env, configStr, config);
        return NULL;
    }

    jint* values =
        (*env)->GetIntArrayElements(env, result, NULL);

    for (jsize i = 0; i < keyCount; i++) {
        jstring keyStr =
            (jstring)(*env)->GetObjectArrayElement(env, keysArray, i);

        if (keyStr == NULL) {
            values[i] = 0;
            continue;
        }

        const char* key =
            (*env)->GetStringUTFChars(env, keyStr, NULL);

        values[i] = extract_value(config, key);

        (*env)->ReleaseStringUTFChars(env, keyStr, key);
        (*env)->DeleteLocalRef(env, keyStr);
    }

    (*env)->ReleaseIntArrayElements(env, result, values, 0);
    (*env)->ReleaseStringUTFChars(env, configStr, config);

    return result;
}
