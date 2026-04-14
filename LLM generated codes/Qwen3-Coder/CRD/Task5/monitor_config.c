// monitor_config.c
#include "monitor_config.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/sysinfo.h>
#include <sys/statvfs.h>
#include <time.h>
#include <sys/time.h>
#include <errno.h>
#include <fcntl.h>
#include <sys/utsname.h>
#include <sys/wait.h>

// Helper function to convert jstring to char*
char* jstringToChar(JNIEnv* env, jstring jstr) {
    const char* cstr = (*env)->GetStringUTFChars(env, jstr, NULL);
    char* result = malloc(strlen(cstr) + 1);
    strcpy(result, cstr);
    (*env)->ReleaseStringUTFChars(env, jstr, cstr);
    return result;
}

// System monitoring implementations
JNIEXPORT jlong JNICALL Java_SystemMonitor_getFreeMemory(JNIEnv *env, jobject obj) {
    struct sysinfo info;
    if (sysinfo(&info) == 0) {
        return (jlong)info.freeram * info.mem_unit;
    }
    return 0;
}

JNIEXPORT jlong JNICALL Java_SystemMonitor_getTotalMemory(JNIEnv *env, jobject obj) {
    struct sysinfo info;
    if (sysinfo(&info) == 0) {
        return (jlong)info.totalram * info.mem_unit;
    }
    return 0;
}

JNIEXPORT jdouble JNICALL Java_SystemMonitor_getCpuUsage(JNIEnv *env, jobject obj) {
    FILE* file = fopen("/proc/loadavg", "r");
    if (file == NULL) {
        return 0.0;
    }
    
    double load_avg;
    fscanf(file, "%lf", &load_avg);
    fclose(file);
    
    // Get number of CPU cores to normalize
    int num_cores = sysconf(_SC_NPROCESSORS_ONLN);
    if (num_cores > 0) {
        load_avg = (load_avg / num_cores) * 100;
    }
    
    return load_avg;
}

JNIEXPORT jstring JNICALL Java_SystemMonitor_getSystemLoadAverage(JNIEnv *env, jobject obj) {
    FILE* file = fopen("/proc/loadavg", "r");
    if (file == NULL) {
        return (*env)->NewStringUTF(env, "0.00 0.00 0.00");
    }
    
    char buffer[256];
    fgets(buffer, sizeof(buffer), file);
    fclose(file);
    
    // Remove newline character
    char* newline = strchr(buffer, '\n');
    if (newline) {
        *newline = '\0';
    }
    
    return (*env)->NewStringUTF(env, buffer);
}

JNIEXPORT jlong JNICALL Java_SystemMonitor_getDiskUsage(JNIEnv *env, jobject obj) {
    struct statvfs buf;
    if (statvfs("/", &buf) == 0) {
        return (jlong)buf.f_blocks * buf.f_frsize;
    }
    return 0;
}

JNIEXPORT jstring JNICALL Java_SystemMonitor_getNetworkStatus(JNIEnv *env, jobject obj) {
    // Simple network status check
    FILE* file = popen("ifconfig | grep 'inet ' | wc -l", "r");
    if (file == NULL) {
        return (*env)->NewStringUTF(env, "UNKNOWN");
    }
    
    char buffer[10];
    fgets(buffer, sizeof(buffer), file);
    pclose(file);
    
    int interfaces = atoi(buffer);
    if (interfaces > 0) {
        return (*env)->NewStringUTF(env, "ACTIVE");
    } else {
        return (*env)->NewStringUTF(env, "INACTIVE");
    }
}

JNIEXPORT jint JNICALL Java_SystemMonitor_getRunningProcesses(JNIEnv *env, jobject obj) {
    struct sysinfo info;
    if (sysinfo(&info) == 0) {
        return (jint)info.procs;
    }
    return 0;
}

JNIEXPORT jstring JNICALL Java_SystemMonitor_getSystemUptime(JNIEnv *env, jobject obj) {
    struct sysinfo info;
    if (sysinfo(&info) == 0) {
        long uptime_seconds = info.uptime;
        int days = uptime_seconds / 86400;
        int hours = (uptime_seconds % 86400) / 3600;
        int minutes = (uptime_seconds % 3600) / 60;
        
        char* uptime_str = malloc(256);
        snprintf(uptime_str, 256, "%d days, %d hours, %d minutes", days, hours, minutes);
        
        jstring result = (*env)->NewStringUTF(env, uptime_str);
        free(uptime_str);
        return result;
    }
    return (*env)->NewStringUTF(env, "Unknown");
}

// Configuration management implementations
JNIEXPORT jboolean JNICALL Java_SystemMonitor_saveConfiguration(JNIEnv *env, jobject obj, jstring configPath, jstring configData) {
    char* path = jstringToChar(env, configPath);
    char* data = jstringToChar(env, configData);
    
    FILE* file = fopen(path, "w");
    if (file == NULL) {
        free(path);
        free(data);
        return JNI_FALSE;
    }
    
    fprintf(file, "%s", data);
    fclose(file);
    
    free(path);
    free(data);
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL Java_SystemMonitor_loadConfiguration(JNIEnv *env, jobject obj, jstring configPath) {
    char* path = jstringToChar(env, configPath);
    
    FILE* file = fopen(path, "r");
    if (file == NULL) {
        free(path);
        return NULL;
    }
    
    // Get file size
    fseek(file, 0, SEEK_END);
    long file_size = ftell(file);
    fseek(file, 0, SEEK_SET);
    
    if (file_size <= 0) {
        fclose(file);
        free(path);
        return (*env)->NewStringUTF(env, "");
    }
    
    char* buffer = malloc(file_size + 1);
    fread(buffer, 1, file_size, file);
    buffer[file_size] = '\0';
    
    fclose(file);
    free(path);
    
    jstring result = (*env)->NewStringUTF(env, buffer);
    free(buffer);
    return result;
}

JNIEXPORT jboolean JNICALL Java_SystemMonitor_updateConfigurationValue(JNIEnv *env, jobject obj, jstring configPath, jstring key, jstring value) {
    char* path = jstringToChar(env, configPath);
    char* key_str = jstringToChar(env, key);
    char* value_str = jstringToChar(env, value);
    
    // Read current configuration
    FILE* file = fopen(path, "r");
    if (file == NULL) {
        free(path);
        free(key_str);
        free(value_str);
        return JNI_FALSE;
    }
    
    // Get file size
    fseek(file, 0, SEEK_END);
    long file_size = ftell(file);
    fseek(file, 0, SEEK_SET);
    
    char* content = malloc(file_size + 1024); // Extra space for potential growth
    fread(content, 1, file_size, file);
    content[file_size] = '\0';
    fclose(file);
    
    // Find and replace the key-value pair
    char* new_content = malloc(file_size + 1024);
    strcpy(new_content, "");
    
    char* line = strtok(content, "\n");
    int found = 0;
    
    while (line != NULL) {
        char* trimmed_line = line;
        // Skip leading whitespace
        while (*trimmed_line == ' ' || *trimmed_line == '\t') {
            trimmed_line++;
        }
        
        if (strncmp(trimmed_line, key_str, strlen(key_str)) == 0 && 
            trimmed_line[strlen(key_str)] == '=') {
            // Found the key, replace with new value
            strcat(new_content, key_str);
            strcat(new_content, "=");
            strcat(new_content, value_str);
            strcat(new_content, "\n");
            found = 1;
        } else {
            strcat(new_content, line);
            strcat(new_content, "\n");
        }
        
        line = strtok(NULL, "\n");
    }
    
    // If key was not found, append it
    if (!found) {
        strcat(new_content, key_str);
        strcat(new_content, "=");
        strcat(new_content, value_str);
        strcat(new_content, "\n");
    }
    
    // Write back to file
    FILE* out_file = fopen(path, "w");
    if (out_file == NULL) {
        free(path);
        free(key_str);
        free(value_str);
        free(content);
        free(new_content);
        return JNI_FALSE;
    }
    
    fprintf(out_file, "%s", new_content);
    fclose(out_file);
    
    free(path);
    free(key_str);
    free(value_str);
    free(content);
    free(new_content);
    
    return JNI_TRUE;
}

JNIEXPORT jstring JNICALL Java_SystemMonitor_getConfigurationValue(JNIEnv *env, jobject obj, jstring configPath, jstring key) {
    char* path = jstringToChar(env, configPath);
    char* key_str = jstringToChar(env, key);
    
    FILE* file = fopen(path, "r");
    if (file == NULL) {
        free(path);
        free(key_str);
        return NULL;
    }
    
    // Get file size
    fseek(file, 0, SEEK_END);
    long file_size = ftell(file);
    fseek(file, 0, SEEK_SET);
    
    char* content = malloc(file_size + 1);
    fread(content, 1, file_size, file);
    content[file_size] = '\0';
    fclose(file);
    
    // Search for the key
    char* line = strtok(content, "\n");
    while (line != NULL) {
        char* trimmed_line = line;
        // Skip leading whitespace
        while (*trimmed_line == ' ' || *trimmed_line == '\t') {
            trimmed_line++;
        }
        
        // Skip comments
        if (*trimmed_line == '#') {
            line = strtok(NULL, "\n");
            continue;
        }
        
        if (strncmp(trimmed_line, key_str, strlen(key_str)) == 0 && 
            trimmed_line[strlen(key_str)] == '=') {
            // Found the key, extract the value
            char* value_start = trimmed_line + strlen(key_str) + 1;
            // Skip leading spaces in value
            while (*value_start == ' ') {
                value_start++;
            }
            
            free(path);
            free(key_str);
            free(content);
            
            return (*env)->NewStringUTF(env, value_start);
        }
        
        line = strtok(NULL, "\n");
    }
    
    free(path);
    free(key_str);
    free(content);
    return NULL;
}

JNIEXPORT jobjectArray JNICALL Java_SystemMonitor_listConfigurationKeys(JNIEnv *env, jobject obj, jstring configPath) {
    char* path = jstringToChar(env, configPath);
    
    FILE* file = fopen(path, "r");
    if (file == NULL) {
        free(path);
        return NULL;
    }
    
    // Get file size
    fseek(file, 0, SEEK_END);
    long file_size = ftell(file);
    fseek(file, 0, SEEK_SET);
    
    char* content = malloc(file_size + 1);
    fread(content, 1, file_size, file);
    content[file_size] = '\0';
    fclose(file);
    
    // Count the number of keys first
    int key_count = 0;
    char* content_copy = malloc(file_size + 1);
    strcpy(content_copy, content);
    
    char* line = strtok(content_copy, "\n");
    while (line != NULL) {
        char* trimmed_line = line;
        // Skip leading whitespace
        while (*trimmed_line == ' ' || *trimmed_line == '\t') {
            trimmed_line++;
        }
        
        // Skip comments and empty lines
        if (*trimmed_line == '#' || *trimmed_line == '\0') {
            line = strtok(NULL, "\n");
            continue;
        }
        
        char* equals_pos = strchr(trimmed_line, '=');
        if (equals_pos != NULL) {
            key_count++;
        }
        
        line = strtok(NULL, "\n");
    }
    
    free(content_copy);
    
    // Create array of strings
    jclass stringClass = (*env)->FindClass(env, "java/lang/String");
    jobjectArray result = (*env)->NewObjectArray(env, key_count, stringClass, NULL);
    
    // Extract keys again and put them in the array
    char* temp_content = malloc(file_size + 1);
    strcpy(temp_content, content);
    
    line = strtok(temp_content, "\n");
    int index = 0;
    
    while (line != NULL && index < key_count) {
        char* trimmed_line = line;
        // Skip leading whitespace
        while (*trimmed_line == ' ' || *trimmed_line == '\t') {
            trimmed_line++;
        }
        
        // Skip comments and empty lines
        if (*trimmed_line == '#' || *trimmed_line == '\0') {
            line = strtok(NULL, "\n");
            continue;
        }
        
        char* equals_pos = strchr(trimmed_line, '=');
        if (equals_pos != NULL) {
            int key_length = equals_pos - trimmed_line;
            char* key = malloc(key_length + 1);
            strncpy(key, trimmed_line, key_length);
            key[key_length] = '\0';
            
            jstring keyString = (*env)->NewStringUTF(env, key);
            (*env)->SetObjectArrayElement(env, result, index, keyString);
            
            free(key);
            index++;
        }
        
        line = strtok(NULL, "\n");
    }
    
    free(temp_content);
    free(content);
    free(path);
    
    return result;
}

JNIEXPORT jboolean JNICALL Java_SystemMonitor_validateConfiguration(JNIEnv *env, jobject obj, jstring configPath) {
    char* path = jstringToChar(env, configPath);
    
    FILE* file = fopen(path, "r");
    if (file == NULL) {
        free(path);
        return JNI_FALSE;
    }
    
    fclose(file);
    free(path);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_SystemMonitor_backupConfiguration(JNIEnv *env, jobject obj, jstring configPath, jstring backupPath) {
    char* src_path = jstringToChar(env, configPath);
    char* dest_path = jstringToChar(env, backupPath);
    
    FILE* src = fopen(src_path, "rb");
    if (src == NULL) {
        free(src_path);
        free(dest_path);
        return JNI_FALSE;
    }
    
    FILE* dest = fopen(dest_path, "wb");
    if (dest == NULL) {
        fclose(src);
        free(src_path);
        free(dest_path);
        return JNI_FALSE;
    }
    
    // Copy file content
    char buffer[4096];
    size_t bytes_read;
    while ((bytes_read = fread(buffer, 1, sizeof(buffer), src)) > 0) {
        fwrite(buffer, 1, bytes_read, dest);
    }
    
    fclose(src);
    fclose(dest);
    
    free(src_path);
    free(dest_path);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_SystemMonitor_restoreConfiguration(JNIEnv *env, jobject obj, jstring backupPath, jstring configPath) {
    char* src_path = jstringToChar(env, backupPath);
    char* dest_path = jstringToChar(env, configPath);
    
    FILE* src = fopen(src_path, "rb");
    if (src == NULL) {
        free(src_path);
        free(dest_path);
        return JNI_FALSE;
    }
    
    FILE* dest = fopen(dest_path, "wb");
    if (dest == NULL) {
        fclose(src);
        free(src_path);
        free(dest_path);
        return JNI_FALSE;
    }
    
    // Copy file content
    char buffer[4096];
    size_t bytes_read;
    while ((bytes_read = fread(buffer, 1, sizeof(buffer), src)) > 0) {
        fwrite(buffer, 1, bytes_read, dest);
    }
    
    fclose(src);
    fclose(dest);
    
    free(src_path);
    free(dest_path);
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_SystemMonitor_encryptConfiguration(JNIEnv *env, jobject obj, jstring configPath, jstring password) {
    // Simple XOR encryption for demonstration
    char* path = jstringToChar(env, configPath);
    char* pwd = jstringToChar(env, password);
    
    FILE* file = fopen(path, "r+");
    if (file == NULL) {
        free(path);
        free(pwd);
        return JNI_FALSE;
    }
    
    // Get file size
    fseek(file, 0, SEEK_END);
    long file_size = ftell(file);
    fseek(file, 0, SEEK_SET);
    
    char* content = malloc(file_size);
    fread(content, 1, file_size, file);
    
    // Simple XOR encryption
    for (long i = 0; i < file_size; i++) {
        content[i] ^= pwd[i % strlen(pwd)];
    }
    
    // Write back encrypted content
    rewind(file);
    fwrite(content, 1, file_size, file);
    fclose(file);
    
    free(content);
    free(path);
    free(pwd);
    
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_SystemMonitor_decryptConfiguration(JNIEnv *env, jobject obj, jstring configPath, jstring password) {
    // Simple XOR decryption (same as encryption)
    char* path = jstringToChar(env, configPath);
    char* pwd = jstringToChar(env, password);
    
    FILE* file = fopen(path, "r+");
    if (file == NULL) {
        free(path);
        free(pwd);
        return JNI_FALSE;
    }
    
    // Get file size
    fseek(file, 0, SEEK_END);
    long file_size = ftell(file);
    fseek(file, 0, SEEK_SET);
    
    char* content = malloc(file_size);
    fread(content, 1, file_size, file);
    
    // Simple XOR decryption (same as encryption)
    for (long i = 0; i < file_size; i++) {
        content[i] ^= pwd[i % strlen(pwd)];
    }
    
    // Write back decrypted content
    rewind(file);
    fwrite(content, 1, file_size, file);
    fclose(file);
    
    free(content);
    free(path);
    free(pwd);
    
    return JNI_TRUE;
}