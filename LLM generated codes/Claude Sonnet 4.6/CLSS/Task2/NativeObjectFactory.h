#ifndef NATIVE_OBJECT_FACTORY_H
#define NATIVE_OBJECT_FACTORY_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * JNI implementation for NativeObjectFactory.createDataContainer().
 *
 * Java signature : public native DataContainer createDataContainer(float value);
 * JNI descriptor : (F)Lcom/example/jni/DataContainer;
 *
 * @param env    The JNI environment pointer.
 * @param thiz   The NativeObjectFactory Java object instance.
 * @param value  The float value forwarded to the DataContainer constructor.
 * @return       A new DataContainer jobject, or nullptr on failure.
 */
JNIEXPORT jobject JNICALL
Java_com_example_jni_NativeObjectFactory_createDataContainer(
    JNIEnv* env,
    jobject thiz,
    jfloat  value
);

#ifdef __cplusplus
}
#endif

#endif // NATIVE_OBJECT_FACTORY_H