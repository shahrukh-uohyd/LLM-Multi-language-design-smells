#include <jni.h>

extern "C" {

// C++ Implementation for Audio Metadata
JNIEXPORT jstring JNICALL Java_MultimediaNativeUtils_00024AudioMetadataExtractor_extractMetadata
  (JNIEnv *env, jobject /* this */, jobject audioStream, jint streamSize) {
    
    // Get direct pointer to the Java ByteBuffer's memory
    void* bufferPtr = env->GetDirectBufferAddress(audioStream);
    
    // ... Pass bufferPtr to your C++ audio parsing library (e.g., FFmpeg) ...
    
    const char* resultJson = "{\"codec\":\"mp3\", \"sampleRate\":44100}";
    return env->NewStringUTF(resultJson);
}

// C++ Implementation for Video Rotation
JNIEXPORT void JNICALL Java_MultimediaNativeUtils_00024VideoFrameProcessor_rotateFrame
  (JNIEnv *env, jobject /* this */, jobject sourceFrame, jobject destFrame, 
   jint width, jint height, jint rotationDegrees) {
    
    // Get direct pointers for both the read and write buffers
    unsigned char* srcPtr = (unsigned char*) env->GetDirectBufferAddress(sourceFrame);
    unsigned char* dstPtr = (unsigned char*) env->GetDirectBufferAddress(destFrame);
    
    // ... Perform the C++ matrix rotation from srcPtr to dstPtr ...
}

}