// platform_compression.c
#include "platform_compression.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/utsname.h>
#include <pwd.h>
#include <uuid/uuid.h>
#include <zlib.h>

// Helper function to convert jstring to char*
char* jstringToChar(JNIEnv* env, jstring jstr) {
    const char* cstr = (*env)->GetStringUTFChars(env, jstr, NULL);
    char* result = malloc(strlen(cstr) + 1);
    strcpy(result, cstr);
    (*env)->ReleaseStringUTFChars(env, jstr, cstr);
    return result;
}

// Helper function to convert jbyteArray to unsigned char*
unsigned char* jbyteArrayToUnsignedChar(JNIEnv* env, jbyteArray array) {
    jsize length = (*env)->GetArrayLength(env, array);
    unsigned char* result = malloc(length);
    
    jbyte* elements = (*env)->GetByteArrayElements(env, array, NULL);
    memcpy(result, elements, length);
    (*env)->ReleaseByteArrayElements(env, array, elements, JNI_ABORT);
    
    return result;
}

// Helper function to convert unsigned char* to jbyteArray
jbyteArray unsignedCharToJByteArray(JNIEnv* env, const unsigned char* data, int length) {
    jbyteArray result = (*env)->NewByteArray(env, length);
    (*env)->SetByteArrayRegion(env, result, 0, length, (const jbyte*)data);
    return result;
}

// Platform utility implementations
JNIEXPORT jstring JNICALL Java_PlatformUtils_getPlatformName(JNIEnv *env, jobject obj) {
    struct utsname sysinfo;
    if (uname(&sysinfo) == 0) {
        return (*env)->NewStringUTF(env, sysinfo.sysname);
    }
    return (*env)->NewStringUTF(env, "Unknown");
}

JNIEXPORT jstring JNICALL Java_PlatformUtils_getSystemArchitecture(JNIEnv *env, jobject obj) {
    struct utsname sysinfo;
    if (uname(&sysinfo) == 0) {
        return (*env)->NewStringUTF(env, sysinfo.machine);
    }
    return (*env)->NewStringUTF(env, "Unknown");
}

JNIEXPORT jlong JNICALL Java_PlatformUtils_getAvailableMemory(JNIEnv *env, jobject obj) {
    long pages = sysconf(_SC_AVPHYS_PAGES);
    long page_size = sysconf(_SC_PAGE_SIZE);
    return (jlong)(pages * page_size);
}

JNIEXPORT jstring JNICALL Java_PlatformUtils_getTempDirectory(JNIEnv *env, jobject obj) {
    const char* temp_dir = getenv("TMPDIR");
    if (temp_dir == NULL) {
        temp_dir = "/tmp";
    }
    return (*env)->NewStringUTF(env, temp_dir);
}

JNIEXPORT jboolean JNICALL Java_PlatformUtils_createProcess(JNIEnv *env, jobject obj, jstring command) {
    char* cmd = jstringToChar(env, command);
    int result = system(cmd);
    free(cmd);
    return (result == 0) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jstring JNICALL Java_PlatformUtils_getUserName(JNIEnv *env, jobject obj) {
    struct passwd* pwd = getpwuid(getuid());
    if (pwd != NULL) {
        return (*env)->NewStringUTF(env, pwd->pw_name);
    }
    return (*env)->NewStringUTF(env, "unknown");
}

JNIEXPORT jstring JNICALL Java_PlatformUtils_getMachineId(JNIEnv *env, jobject obj) {
    uuid_t machine_uuid;
    char uuid_str[37];
    uuid_generate(machine_uuid);
    uuid_unparse(machine_uuid, uuid_str);
    return (*env)->NewStringUTF(env, uuid_str);
}

// Compression implementations
JNIEXPORT jbyteArray JNICALL Java_PlatformUtils_compressData(JNIEnv *env, jobject obj, jbyteArray data, jstring algorithm) {
    char* algo = jstringToChar(env, algorithm);
    unsigned char* input_data = jbyteArrayToUnsignedChar(env, data);
    jsize input_len = (*env)->GetArrayLength(env, data);
    
    z_stream strm = {0};
    int ret;
    unsigned char* output_buffer = NULL;
    int output_capacity = 0;
    int output_size = 0;
    
    if (strcmp(algo, "GZIP") == 0) {
        ret = deflateInit2(&strm, Z_DEFAULT_COMPRESSION, Z_DEFLATED, 15 + 16, 8, Z_DEFAULT_STRATEGY);
    } else if (strcmp(algo, "DEFLATE") == 0) {
        ret = deflateInit(&strm, Z_DEFAULT_COMPRESSION);
    } else {
        free(algo);
        free(input_data);
        return NULL;
    }
    
    if (ret != Z_OK) {
        free(algo);
        free(input_data);
        return NULL;
    }
    
    output_capacity = input_len * 2; // Initial guess for output size
    output_buffer = malloc(output_capacity);
    
    strm.next_in = input_data;
    strm.avail_in = input_len;
    strm.next_out = output_buffer;
    strm.avail_out = output_capacity;
    
    ret = deflate(&strm, Z_SYNC_FLUSH);
    if (ret != Z_OK && ret != Z_STREAM_END) {
        deflateEnd(&strm);
        free(algo);
        free(input_data);
        free(output_buffer);
        return NULL;
    }
    
    output_size = output_capacity - strm.avail_out;
    
    deflateEnd(&strm);
    free(algo);
    free(input_data);
    
    jbyteArray result = unsignedCharToJByteArray(env, output_buffer, output_size);
    free(output_buffer);
    
    return result;
}

JNIEXPORT jbyteArray JNICALL Java_PlatformUtils_decompressData(JNIEnv *env, jobject obj, jbyteArray compressedData, jstring algorithm) {
    char* algo = jstringToChar(env, algorithm);
    unsigned char* input_data = jbyteArrayToUnsignedChar(env, compressedData);
    jsize input_len = (*env)->GetArrayLength(env, compressedData);
    
    z_stream strm = {0};
    int ret;
    unsigned char* output_buffer = NULL;
    int output_capacity = input_len * 10; // Initial guess for output size
    int output_size = 0;
    
    if (strcmp(algo, "GZIP") == 0) {
        ret = inflateInit2(&strm, 15 + 16);
    } else if (strcmp(algo, "DEFLATE") == 0) {
        ret = inflateInit(&strm);
    } else {
        free(algo);
        free(input_data);
        return NULL;
    }
    
    if (ret != Z_OK) {
        free(algo);
        free(input_data);
        return NULL;
    }
    
    output_buffer = malloc(output_capacity);
    
    strm.next_in = input_data;
    strm.avail_in = input_len;
    strm.next_out = output_buffer;
    strm.avail_out = output_capacity;
    
    do {
        ret = inflate(&strm, Z_SYNC_FLUSH);
        if (ret == Z_BUF_ERROR) {
            // Need more space, expand buffer
            output_capacity *= 2;
            unsigned char* new_buffer = realloc(output_buffer, output_capacity);
            if (new_buffer == NULL) {
                inflateEnd(&strm);
                free(algo);
                free(input_data);
                free(output_buffer);
                return NULL;
            }
            output_buffer = new_buffer;
            strm.next_out = output_buffer + strm.total_out;
            strm.avail_out = output_capacity - strm.total_out;
        } else if (ret != Z_OK && ret != Z_STREAM_END) {
            inflateEnd(&strm);
            free(algo);
            free(input_data);
            free(output_buffer);
            return NULL;
        }
    } while (ret != Z_STREAM_END);
    
    output_size = strm.total_out;
    
    inflateEnd(&strm);
    free(algo);
    free(input_data);
    
    jbyteArray result = unsignedCharToJByteArray(env, output_buffer, output_size);
    free(output_buffer);
    
    return result;
}

JNIEXPORT jint JNICALL Java_PlatformUtils_getCompressionRatio(JNIEnv *env, jobject obj, jbyteArray originalData, jbyteArray compressedData) {
    jsize orig_len = (*env)->GetArrayLength(env, originalData);
    jsize comp_len = (*env)->GetArrayLength(env, compressedData);
    
    if (orig_len == 0) {
        return 0;
    }
    
    // Calculate ratio as percentage
    int ratio = (int)((double)comp_len / orig_len * 100);
    return ratio;
}

JNIEXPORT jbyteArray JNICALL Java_PlatformUtils_compressWithLevel(JNIEnv *env, jobject obj, jbyteArray data, jint compressionLevel) {
    unsigned char* input_data = jbyteArrayToUnsignedChar(env, data);
    jsize input_len = (*env)->GetArrayLength(env, data);
    
    z_stream strm = {0};
    int ret;
    unsigned char* output_buffer = NULL;
    int output_capacity = 0;
    int output_size = 0;
    
    // Clamp compression level between 1 and 9
    if (compressionLevel < 1) compressionLevel = 1;
    if (compressionLevel > 9) compressionLevel = 9;
    
    ret = deflateInit(&strm, compressionLevel);
    if (ret != Z_OK) {
        free(input_data);
        return NULL;
    }
    
    output_capacity = input_len * 2; // Initial guess for output size
    output_buffer = malloc(output_capacity);
    
    strm.next_in = input_data;
    strm.avail_in = input_len;
    strm.next_out = output_buffer;
    strm.avail_out = output_capacity;
    
    ret = deflate(&strm, Z_SYNC_FLUSH);
    if (ret != Z_OK && ret != Z_STREAM_END) {
        deflateEnd(&strm);
        free(input_data);
        free(output_buffer);
        return NULL;
    }
    
    output_size = output_capacity - strm.avail_out;
    
    deflateEnd(&strm);
    free(input_data);
    
    jbyteArray result = unsignedCharToJByteArray(env, output_buffer, output_size);
    free(output_buffer);
    
    return result;
}

JNIEXPORT jboolean JNICALL Java_PlatformUtils_validateCompressedData(JNIEnv *env, jobject obj, jbyteArray compressedData) {
    unsigned char* input_data = jbyteArrayToUnsignedChar(env, compressedData);
    jsize input_len = (*env)->GetArrayLength(env, compressedData);
    
    z_stream strm = {0};
    int ret;
    unsigned char* output_buffer = NULL;
    int output_capacity = input_len * 10; // Initial guess for output size
    
    // Try to decompress to validate
    ret = inflateInit(&strm);
    if (ret != Z_OK) {
        free(input_data);
        return JNI_FALSE;
    }
    
    output_buffer = malloc(output_capacity);
    
    strm.next_in = input_data;
    strm.avail_in = input_len;
    strm.next_out = output_buffer;
    strm.avail_out = output_capacity;
    
    do {
        ret = inflate(&strm, Z_SYNC_FLUSH);
        if (ret == Z_BUF_ERROR) {
            // Need more space, expand buffer
            output_capacity *= 2;
            unsigned char* new_buffer = realloc(output_buffer, output_capacity);
            if (new_buffer == NULL) {
                inflateEnd(&strm);
                free(input_data);
                free(output_buffer);
                return JNI_FALSE;
            }
            output_buffer = new_buffer;
            strm.next_out = output_buffer + strm.total_out;
            strm.avail_out = output_capacity - strm.total_out;
        } else if (ret != Z_OK && ret != Z_STREAM_END) {
            inflateEnd(&strm);
            free(input_data);
            free(output_buffer);
            return JNI_FALSE;
        }
    } while (ret != Z_STREAM_END);
    
    inflateEnd(&strm);
    free(input_data);
    free(output_buffer);
    
    return JNI_TRUE;
}