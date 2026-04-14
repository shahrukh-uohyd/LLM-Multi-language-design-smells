#include <jni.h>
#include <stdio.h>

// Existing C implementation for PlatformUtils
JNIEXPORT void JNICALL Java_PlatformUtils_executeSystemCommand(JNIEnv *env, jobject thisObj, jstring command) {
    const char *cmd_str = (*env)->GetStringUTFChars(env, command, NULL);
    printf("Executing command: %s\n", cmd_str);
    // Execute command logic...
    (*env)->ReleaseStringUTFChars(env, command, cmd_str);
}