/*
 * config_native.c
 *
 * JNI implementation of com.app.config.ConfigNative.
 *
 * Storage  : INI-style file (sections + key=value pairs)
 * Threading: single global config store guarded by a POSIX mutex
 * Regex    : POSIX ERE via <regex.h>  (no external deps)
 * Atomic save: write to "<path>.tmp" then rename(2)
 *
 * Compile (Linux):
 *   gcc -std=c11 -shared -fPIC -O2 -Wall -Wextra        \
 *       -I"${JAVA_HOME}/include"                          \
 *       -I"${JAVA_HOME}/include/linux"                    \
 *       config_native.c                                   \
 *       -lpthread                                         \
 *       -o libconfig_native.so
 */

#include <jni.h>
#include <pthread.h>
#include <regex.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>
#include <ctype.h>

/* ── tuneable limits ────────────────────────────────────────────────────── */
#define MAX_SECTIONS      128
#define MAX_KEYS_PER_SEC  256
#define MAX_KEY_LEN       256
#define MAX_VAL_LEN       4096
#define MAX_LINE_LEN      (MAX_KEY_LEN + MAX_VAL_LEN + 8)
#define MAX_FILEPATH_LEN  1024

/* ── JNI class/exception paths ──────────────────────────────────────────── */
#define CONFIG_EXCEPTION  "com/app/config/ConfigException"
#define IAE_CLASS         "java/lang/IllegalArgumentException"
#define OOM_CLASS         "java/lang/OutOfMemoryError"

/* ── Operation ordinals (must mirror ConfigException.Operation enum) ─────── */
#define OP_LOAD       0
#define OP_SAVE       1
#define OP_GET_VALUE  2
#define OP_SET_VALUE  3
#define OP_DELETE_KEY 4
#define OP_VALIDATE   5
#define OP_RELOAD     6
#define OP_UNKNOWN    7

/* ═══════════════════════════════════════════════════════════════════════════
 * In-memory config store
 * ═══════════════════════════════════════════════════════════════════════════ */

typedef struct {
    char key[MAX_KEY_LEN];
    char value[MAX_VAL_LEN];
} ConfigEntry;

typedef struct {
    char        name[MAX_KEY_LEN];
    ConfigEntry entries[MAX_KEYS_PER_SEC];
    int         entry_count;
} ConfigSection;

typedef struct {
    ConfigSection sections[MAX_SECTIONS];
    int           section_count;
    char          loaded_path[MAX_FILEPATH_LEN];
} ConfigStore;

/* ── global state protected by a mutex ─────────────────────────────────── */
static ConfigStore   g_store;
static pthread_mutex_t g_mutex = PTHREAD_MUTEX_INITIALIZER;

/* ═══════════════════════════════════════════════════════════════════════════
 * Exception helpers
 * ════��══════════════════════════════════════════════════════════════════════ */

static void throw_iae(JNIEnv *env, const char *msg) {
    jclass cls = (*env)->FindClass(env, IAE_CLASS);
    if (cls) { (*env)->ThrowNew(env, cls, msg); (*env)->DeleteLocalRef(env, cls); }
}

static void throw_oom(JNIEnv *env, const char *msg) {
    jclass cls = (*env)->FindClass(env, OOM_CLASS);
    if (cls) { (*env)->ThrowNew(env, cls, msg); (*env)->DeleteLocalRef(env, cls); }
}

/*
 * Throws ConfigException(String message, int errorCode, int operationOrdinal).
 * Mirrors the 3-arg constructor in ConfigException.java.
 */
static void throw_config_exception(JNIEnv *env, const char *message,
                                    int errorCode, int operationOrdinal) {
    jclass cls = (*env)->FindClass(env, CONFIG_EXCEPTION);
    if (!cls) return;

    jmethodID ctor = (*env)->GetMethodID(env, cls, "<init>",
                                          "(Ljava/lang/String;II)V");
    if (!ctor) { (*env)->DeleteLocalRef(env, cls); return; }

    jstring jmsg = (*env)->NewStringUTF(env, message ? message : "unknown config error");
    if (!jmsg) { (*env)->DeleteLocalRef(env, cls); return; }

    jobject ex = (*env)->NewObject(env, cls, ctor, jmsg,
                                    (jint)errorCode, (jint)operationOrdinal);
    if (ex) {
        (*env)->Throw(env, (jthrowable)ex);
        (*env)->DeleteLocalRef(env, ex);
    }
    (*env)->DeleteLocalRef(env, jmsg);
    (*env)->DeleteLocalRef(env, cls);
}

/* ═══════════════════════════════════════════════════════════════════════════
 * Internal store helpers
 * ═══════════════════════════════════════════════════════════════════════════ */

/* Returns pointer to an existing section, or NULL if not found. */
static ConfigSection *find_section(ConfigStore *store, const char *name) {
    for (int i = 0; i < store->section_count; i++) {
        if (strcmp(store->sections[i].name, name) == 0)
            return &store->sections[i];
    }
    return NULL;
}

/* Returns pointer to an existing section, creating it if needed.
   Returns NULL if the section table is full. */
static ConfigSection *find_or_create_section(ConfigStore *store, const char *name) {
    ConfigSection *sec = find_section(store, name);
    if (sec) return sec;
    if (store->section_count >= MAX_SECTIONS) return NULL;
    sec = &store->sections[store->section_count++];
    memset(sec, 0, sizeof(*sec));
    strncpy(sec->name, name, MAX_KEY_LEN - 1);
    sec->name[MAX_KEY_LEN - 1] = '\0';
    return sec;
}

/* Returns the value string for key in section, or NULL if absent. */
static const char *get_value_internal(ConfigStore *store,
                                       const char *section, const char *key) {
    ConfigSection *sec = find_section(store, section);
    if (!sec) return NULL;
    for (int i = 0; i < sec->entry_count; i++) {
        if (strcmp(sec->entries[i].key, key) == 0)
            return sec->entries[i].value;
    }
    return NULL;
}

/* Sets a value; returns 0 on success, -1 on overflow. */
static int set_value_internal(ConfigStore *store,
                               const char *section, const char *key,
                               const char *value) {
    ConfigSection *sec = find_or_create_section(store, section);
    if (!sec) return -1;

    /* Overwrite if key already exists */
    for (int i = 0; i < sec->entry_count; i++) {
        if (strcmp(sec->entries[i].key, key) == 0) {
            strncpy(sec->entries[i].value, value, MAX_VAL_LEN - 1);
            sec->entries[i].value[MAX_VAL_LEN - 1] = '\0';
            return 0;
        }
    }

    /* Add new key */
    if (sec->entry_count >= MAX_KEYS_PER_SEC) return -1;
    ConfigEntry *e = &sec->entries[sec->entry_count++];
    strncpy(e->key,   key,   MAX_KEY_LEN - 1);  e->key[MAX_KEY_LEN - 1]   = '\0';
    strncpy(e->value, value, MAX_VAL_LEN - 1);  e->value[MAX_VAL_LEN - 1] = '\0';
    return 0;
}

/* Clears all store data (does not free — all storage is static arrays). */
static void clear_store(ConfigStore *store) {
    store->section_count = 0;
    store->loaded_path[0] = '\0';
    memset(store->sections, 0, sizeof(store->sections));
}

/* ── INI string utilities ────────────────────────────────────────────────── */

/* In-place left + right strip of whitespace. Returns pointer into src. */
static char *trim(char *s) {
    while (isspace((unsigned char)*s)) s++;
    if (*s == '\0') return s;
    char *end = s + strlen(s) - 1;
    while (end > s && isspace((unsigned char)*end)) end--;
    *(end + 1) = '\0';
    return s;
}

/* Remove inline comment starting with ';' or '#'. Modifies buffer in place. */
static void strip_inline_comment(char *s) {
    int in_quotes = 0;
    for (char *p = s; *p; p++) {
        if (*p == '"') { in_quotes = !in_quotes; continue; }
        if (!in_quotes && (*p == ';' || *p == '#')) { *p = '\0'; return; }
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 * INI parser
 * ═══════════════════════════════════════════════════════════════════════════ */

/*
 * Parses an INI file into *store.
 * Supports:
 *   [section]
 *   key = value    ; inline comments
 *   # full-line comments
 *   ; full-line comments
 * Returns 0 on success, non-zero on error (sets errno).
 */
static int parse_ini_file(const char *path, ConfigStore *store) {
    FILE *fp = fopen(path, "r");
    if (!fp) return -1;

    char    line[MAX_LINE_LEN];
    char    current_section[MAX_KEY_LEN] = "default";
    int     line_num = 0;

    while (fgets(line, sizeof(line), fp)) {
        line_num++;

        /* Strip trailing newline */
        size_t len = strlen(line);
        if (len > 0 && line[len - 1] == '\n') line[--len] = '\0';
        if (len > 0 && line[len - 1] == '\r') line[--len] = '\0';

        char *p = trim(line);

        /* Skip blank lines and full-line comments */
        if (*p == '\0' || *p == '#' || *p == ';') continue;

        /* Section header */
        if (*p == '[') {
            char *end = strchr(p, ']');
            if (!end) continue; /* malformed — skip */
            *end = '\0';
            char *sec_name = trim(p + 1);
            strncpy(current_section, sec_name, MAX_KEY_LEN - 1);
            current_section[MAX_KEY_LEN - 1] = '\0';

            /* Pre-create the section so listSections() preserves file order */
            find_or_create_section(store, current_section);
            continue;
        }

        /* key = value */
        char *eq = strchr(p, '=');
        if (!eq) continue; /* malformed — skip */
        *eq = '\0';

        char *key   = trim(p);
        char *value = trim(eq + 1);
        strip_inline_comment(value);
        value = trim(value); /* re-trim after comment removal */

        if (*key == '\0') continue;
        set_value_internal(store, current_section, key, value);
    }

    fclose(fp);
    return 0;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * INI writer
 * ═══════════════════════════════════════════════════════════════════════════ */

/*
 * Writes the store to *path atomically (write to "<path>.tmp", then rename).
 * Returns 0 on success, -1 on error (errno set).
 */
static int write_ini_file(const char *path, const ConfigStore *store) {
    char tmp_path[MAX_FILEPATH_LEN + 8];
    snprintf(tmp_path, sizeof(tmp_path), "%s.tmp", path);

    FILE *fp = fopen(tmp_path, "w");
    if (!fp) return -1;

    fprintf(fp, "# Configuration file — auto-generated by config_native\n\n");

    for (int s = 0; s < store->section_count; s++) {
        const ConfigSection *sec = &store->sections[s];
        if (sec->entry_count == 0) continue;

        fprintf(fp, "[%s]\n", sec->name);
        for (int k = 0; k < sec->entry_count; k++) {
            fprintf(fp, "%s = %s\n",
                    sec->entries[k].key,
                    sec->entries[k].value);
        }
        fprintf(fp, "\n");
    }

    if (fflush(fp) != 0 || fclose(fp) != 0) {
        remove(tmp_path);
        return -1;
    }

    /* Atomic rename */
    if (rename(tmp_path, path) != 0) {
        remove(tmp_path);
        return -1;
    }
    return 0;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * Boolean string helper
 * ═══════════════════════════════════════════════════════════════════════════ */

/* Returns 1=true, 0=false, -1=parse error */
static int parse_bool(const char *s) {
    if (!s) return -1;
    if (strcasecmp(s, "true")  == 0 || strcasecmp(s, "yes") == 0 ||
        strcasecmp(s, "on")    == 0 || strcmp(s, "1")       == 0)
        return 1;
    if (strcasecmp(s, "false") == 0 || strcasecmp(s, "no")  == 0 ||
        strcasecmp(s, "off")   == 0 || strcmp(s, "0")       == 0)
        return 0;
    return -1;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * JNI helper: get a UTF-8 C string from a jstring; caller must ReleaseStringUTFChars
 * ═══════════════════════════════════════════════════════════════════════════ */
#define GET_CSTR(env, jstr, cvar)                                          \
    const char *(cvar) = NULL;                                             \
    if ((jstr) == NULL) {                                                  \
        throw_iae(env, #cvar " (jstring) must not be null");               \
        return;                                                            \
    }                                                                      \
    (cvar) = (*env)->GetStringUTFChars(env, (jstr), NULL);                 \
    if (!(cvar)) { throw_oom(env, "GetStringUTFChars failed"); return; }

#define GET_CSTR_RET(env, jstr, cvar, retval)                              \
    const char *(cvar) = NULL;                                             \
    if ((jstr) == NULL) {                                                  \
        throw_iae(env, #cvar " (jstring) must not be null");               \
        return (retval);                                                   \
    }                                                                      \
    (cvar) = (*env)->GetStringUTFChars(env, (jstr), NULL);                 \
    if (!(cvar)) { throw_oom(env, "GetStringUTFChars failed"); return (retval); }

#define RELEASE_CSTR(env, jstr, cvar) \
    (*env)->ReleaseStringUTFChars(env, (jstr), (cvar))

/* ═══════════════════════════════════════════════════════════════════════════
 * loadConfig
 * Java_com_app_config_ConfigNative_loadConfig
 * ═══════════════════════════════════════════════════════════════════════════ */
JNIEXPORT void JNICALL
Java_com_app_config_ConfigNative_loadConfig(JNIEnv *env, jobject thiz, jstring jFilePath) {
    (void)thiz;

    GET_CSTR(env, jFilePath, path);

    if (strlen(path) == 0) {
        RELEASE_CSTR(env, jFilePath, path);
        throw_iae(env, "filePath must not be empty");
        return;
    }

    ConfigStore tmp_store;
    memset(&tmp_store, 0, sizeof(tmp_store));

    int rc = parse_ini_file(path, &tmp_store);
    if (rc != 0) {
        int err = errno;
        char msg[256];
        snprintf(msg, sizeof(msg), "Failed to open/parse config file '%s': %s",
                 path, strerror(err));
        RELEASE_CSTR(env, jFilePath, path);
        throw_config_exception(env, msg, err, OP_LOAD);
        return;
    }

    strncpy(tmp_store.loaded_path, path, MAX_FILEPATH_LEN - 1);
    tmp_store.loaded_path[MAX_FILEPATH_LEN - 1] = '\0';

    pthread_mutex_lock(&g_mutex);
    g_store = tmp_store;
    pthread_mutex_unlock(&g_mutex);

    RELEASE_CSTR(env, jFilePath, path);
}

/* ═══════════════════════════════════════════════════════════════════════════
 * saveConfig
 * Java_com_app_config_ConfigNative_saveConfig
 * ═══════════════════════════════════════════════════════════════════════════ */
JNIEXPORT void JNICALL
Java_com_app_config_ConfigNative_saveConfig(JNIEnv *env, jobject thiz, jstring jFilePath) {
    (void)thiz;

    GET_CSTR(env, jFilePath, path);

    if (strlen(path) == 0) {
        RELEASE_CSTR(env, jFilePath, path);
        throw_iae(env, "filePath must not be empty");
        return;
    }

    pthread_mutex_lock(&g_mutex);
    ConfigStore snapshot = g_store;
    pthread_mutex_unlock(&g_mutex);

    int rc = write_ini_file(path, &snapshot);
    if (rc != 0) {
        int err = errno;
        char msg[256];
        snprintf(msg, sizeof(msg), "Failed to write config file '%s': %s",
                 path, strerror(err));
        RELEASE_CSTR(env, jFilePath, path);
        throw_config_exception(env, msg, err, OP_SAVE);
        return;
    }

    RELEASE_CSTR(env, jFilePath, path);
}

/* ═══════════════════════════════════════════════════════════════════════════
 * getValue
 * Java_com_app_config_ConfigNative_getValue
 * ═══════════════════════════════════════════════════════════════════════════ */
JNIEXPORT jstring JNICALL
Java_com_app_config_ConfigNative_getValue(JNIEnv *env, jobject thiz,
                                           jstring jSection, jstring jKey,
                                           jstring jDefault) {
    (void)thiz;

    GET_CSTR_RET(env, jSection, section, NULL);
    GET_CSTR_RET(env, jKey,     key,     NULL);

    pthread_mutex_lock(&g_mutex);
    const char *val = get_value_internal(&g_store, section, key);
    jstring result = val ? (*env)->NewStringUTF(env, val) : jDefault;
    pthread_mutex_unlock(&g_mutex);

    RELEASE_CSTR(env, jSection, section);
    RELEASE_CSTR(env, jKey,     key);
    return result;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * setValue
 * Java_com_app_config_ConfigNative_setValue
 * ═══════════════════════════════════════════════════════════════════════════ */
JNIEXPORT void JNICALL
Java_com_app_config_ConfigNative_setValue(JNIEnv *env, jobject thiz,
                                           jstring jSection, jstring jKey,
                                           jstring jValue) {
    (void)thiz;

    GET_CSTR(env, jSection, section);
    GET_CSTR(env, jKey,     key);
    GET_CSTR(env, jValue,   value);

    pthread_mutex_lock(&g_mutex);
    int rc = set_value_internal(&g_store, section, key, value);
    pthread_mutex_unlock(&g_mutex);

    RELEASE_CSTR(env, jSection, section);
    RELEASE_CSTR(env, jKey,     key);
    RELEASE_CSTR(env, jValue,   value);

    if (rc != 0) {
        throw_config_exception(env,
            "Config store is full (MAX_SECTIONS or MAX_KEYS_PER_SEC exceeded)",
            0, OP_SET_VALUE);
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
 * deleteKey
 * Java_com_app_config_ConfigNative_deleteKey
 * ═══════════════════════════════════════════════════════════════════════════ */
JNIEXPORT jboolean JNICALL
Java_com_app_config_ConfigNative_deleteKey(JNIEnv *env, jobject thiz,
                                            jstring jSection, jstring jKey) {
    (void)thiz;

    GET_CSTR_RET(env, jSection, section, JNI_FALSE);
    GET_CSTR_RET(env, jKey,     key,     JNI_FALSE);

    jboolean deleted = JNI_FALSE;

    pthread_mutex_lock(&g_mutex);
    ConfigSection *sec = find_section(&g_store, section);
    if (sec) {
        for (int i = 0; i < sec->entry_count; i++) {
            if (strcmp(sec->entries[i].key, key) == 0) {
                /* Compact the array */
                memmove(&sec->entries[i], &sec->entries[i + 1],
                        (sec->entry_count - i - 1) * sizeof(ConfigEntry));
                sec->entry_count--;
                deleted = JNI_TRUE;
                break;
            }
        }
    }
    pthread_mutex_unlock(&g_mutex);

    RELEASE_CSTR(env, jSection, section);
    RELEASE_CSTR(env, jKey,     key);
    return deleted;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * deleteSection
 * Java_com_app_config_ConfigNative_deleteSection
 * ═══════════════════════════════════════════════════════════════════════════ */
JNIEXPORT jboolean JNICALL
Java_com_app_config_ConfigNative_deleteSection(JNIEnv *env, jobject thiz,
                                                jstring jSection) {
    (void)thiz;

    GET_CSTR_RET(env, jSection, section, JNI_FALSE);

    jboolean deleted = JNI_FALSE;

    pthread_mutex_lock(&g_mutex);
    for (int i = 0; i < g_store.section_count; i++) {
        if (strcmp(g_store.sections[i].name, section) == 0) {
            memmove(&g_store.sections[i], &g_store.sections[i + 1],
                    (g_store.section_count - i - 1) * sizeof(ConfigSection));
            g_store.section_count--;
            deleted = JNI_TRUE;
            break;
        }
    }
    pthread_mutex_unlock(&g_mutex);

    RELEASE_CSTR(env, jSection, section);
    return deleted;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * listKeys
 * Java_com_app_config_ConfigNative_listKeys
 * ═══════════════════════════════════════════════════════════════════════════ */
JNIEXPORT jobjectArray JNICALL
Java_com_app_config_ConfigNative_listKeys(JNIEnv *env, jobject thiz, jstring jSection) {
    (void)thiz;

    GET_CSTR_RET(env, jSection, section, NULL);

    jclass    strClass = (*env)->FindClass(env, "java/lang/String");
    jboolean  found    = JNI_FALSE;
    int       count    = 0;
    char      keys[MAX_KEYS_PER_SEC][MAX_KEY_LEN];

    pthread_mutex_lock(&g_mutex);
    ConfigSection *sec = find_section(&g_store, section);
    if (sec) {
        found = JNI_TRUE;
        count = sec->entry_count;
        for (int i = 0; i < count; i++)
            strncpy(keys[i], sec->entries[i].key, MAX_KEY_LEN - 1);
    }
    pthread_mutex_unlock(&g_mutex);

    RELEASE_CSTR(env, jSection, section);

    jobjectArray result = (*env)->NewObjectArray(env, (jsize)count, strClass, NULL);
    if (!result) { throw_oom(env, "failed to allocate listKeys result"); return NULL; }

    for (int i = 0; i < count; i++) {
        jstring jk = (*env)->NewStringUTF(env, keys[i]);
        if (!jk) { throw_oom(env, "NewStringUTF failed in listKeys"); return NULL; }
        (*env)->SetObjectArrayElement(env, result, (jsize)i, jk);
        (*env)->DeleteLocalRef(env, jk);
    }
    return result;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * listSections
 * Java_com_app_config_ConfigNative_listSections
 * ═══════════════════════════════════════════════════════════════════════════ */
JNIEXPORT jobjectArray JNICALL
Java_com_app_config_ConfigNative_listSections(JNIEnv *env, jobject thiz) {
    (void)thiz;

    jclass strClass = (*env)->FindClass(env, "java/lang/String");
    int    count    = 0;
    char   names[MAX_SECTIONS][MAX_KEY_LEN];

    pthread_mutex_lock(&g_mutex);
    count = g_store.section_count;
    for (int i = 0; i < count; i++)
        strncpy(names[i], g_store.sections[i].name, MAX_KEY_LEN - 1);
    pthread_mutex_unlock(&g_mutex);

    jobjectArray result = (*env)->NewObjectArray(env, (jsize)count, strClass, NULL);
    if (!result) { throw_oom(env, "failed to allocate listSections result"); return NULL; }

    for (int i = 0; i < count; i++) {
        jstring js = (*env)->NewStringUTF(env, names[i]);
        if (!js) { throw_oom(env, "NewStringUTF failed in listSections"); return NULL; }
        (*env)->SetObjectArrayElement(env, result, (jsize)i, js);
        (*env)->DeleteLocalRef(env, js);
    }
    return result;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * validateValue
 * Java_com_app_config_ConfigNative_validateValue
 * ═══════════════════════════════════════════════════════════════════════════ */
JNIEXPORT jboolean JNICALL
Java_com_app_config_ConfigNative_validateValue(JNIEnv *env, jobject thiz,
                                                jstring jSection, jstring jKey,
                                                jstring jPattern) {
    (void)thiz;

    GET_CSTR_RET(env, jSection, section, JNI_FALSE);
    GET_CSTR_RET(env, jKey,     key,     JNI_FALSE);
    GET_CSTR_RET(env, jPattern, pattern, JNI_FALSE);

    pthread_mutex_lock(&g_mutex);
    const char *val = get_value_internal(&g_store, section, key);
    char val_copy[MAX_VAL_LEN] = "";
    if (val) strncpy(val_copy, val, MAX_VAL_LEN - 1);
    pthread_mutex_unlock(&g_mutex);

    RELEASE_CSTR(env, jSection, section);
    RELEASE_CSTR(env, jKey,     key);

    if (!val) {
        RELEASE_CSTR(env, jPattern, pattern);
        return JNI_FALSE; /* key absent → does not match */
    }

    regex_t    re;
    int        rc  = regcomp(&re, pattern, REG_EXTENDED | REG_NOSUB);
    jboolean   ret = JNI_FALSE;

    if (rc != 0) {
        char errbuf[256];
        regerror(rc, &re, errbuf, sizeof(errbuf));
        char msg[512];
        snprintf(msg, sizeof(msg), "Invalid regex pattern '%s': %s", pattern, errbuf);
        RELEASE_CSTR(env, jPattern, pattern);
        throw_config_exception(env, msg, rc, OP_VALIDATE);
        return JNI_FALSE;
    }

    ret = (regexec(&re, val_copy, 0, NULL, 0) == 0) ? JNI_TRUE : JNI_FALSE;
    regfree(&re);

    RELEASE_CSTR(env, jPattern, pattern);
    return ret;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * reloadConfig
 * Java_com_app_config_ConfigNative_reloadConfig
 * ═══════════════════════════════════════════════════════════════════════════ */
JNIEXPORT void JNICALL
Java_com_app_config_ConfigNative_reloadConfig(JNIEnv *env, jobject thiz) {
    (void)thiz;

    pthread_mutex_lock(&g_mutex);
    char path[MAX_FILEPATH_LEN];
    strncpy(path, g_store.loaded_path, MAX_FILEPATH_LEN - 1);
    pthread_mutex_unlock(&g_mutex);

    if (path[0] == '\0') {
        throw_config_exception(env,
            "reloadConfig called but no file has been loaded yet",
            0, OP_RELOAD);
        return;
    }

    ConfigStore tmp;
    memset(&tmp, 0, sizeof(tmp));
    int rc = parse_ini_file(path, &tmp);
    if (rc != 0) {
        int err = errno;
        char msg[256];
        snprintf(msg, sizeof(msg),
                 "reloadConfig failed to re-read '%s': %s", path, strerror(err));
        throw_config_exception(env, msg, err, OP_RELOAD);
        return;
    }

    strncpy(tmp.loaded_path, path, MAX_FILEPATH_LEN - 1);
    tmp.loaded_path[MAX_FILEPATH_LEN - 1] = '\0';

    /* Atomically replace store only on success */
    pthread_mutex_lock(&g_mutex);
    g_store = tmp;
    pthread_mutex_unlock(&g_mutex);
}

/* ═════════════════════════════════════════════════════════════���═════════════
 * getIntValue
 * Java_com_app_config_ConfigNative_getIntValue
 * ═══════════════════════════════════════════════════════════════════════════ */
JNIEXPORT jint JNICALL
Java_com_app_config_ConfigNative_getIntValue(JNIEnv *env, jobject thiz,
                                              jstring jSection, jstring jKey,
                                              jint defaultValue) {
    (void)thiz;

    GET_CSTR_RET(env, jSection, section, defaultValue);
    GET_CSTR_RET(env, jKey,     key,     defaultValue);

    pthread_mutex_lock(&g_mutex);
    const char *val = get_value_internal(&g_store, section, key);
    char val_copy[MAX_VAL_LEN] = "";
    if (val) strncpy(val_copy, val, MAX_VAL_LEN - 1);
    pthread_mutex_unlock(&g_mutex);

    RELEASE_CSTR(env, jSection, section);
    RELEASE_CSTR(env, jKey,     key);

    if (!val) return defaultValue;

    char *endptr = NULL;
    errno = 0;
    long parsed = strtol(val_copy, &endptr, 10);
    if (errno != 0 || endptr == val_copy || *endptr != '\0') {
        char msg[256];
        snprintf(msg, sizeof(msg),
                 "Cannot parse '%s' as integer for key '%s'", val_copy, key);
        throw_config_exception(env, msg, errno, OP_GET_VALUE);
        return defaultValue;
    }
    return (jint)parsed;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * getDoubleValue
 * Java_com_app_config_ConfigNative_getDoubleValue
 * ═══════════════════════════════════════════════════════════════════════════ */
JNIEXPORT jdouble JNICALL
Java_com_app_config_ConfigNative_getDoubleValue(JNIEnv *env, jobject thiz,
                                                 jstring jSection, jstring jKey,
                                                 jdouble defaultValue) {
    (void)thiz;

    GET_CSTR_RET(env, jSection, section, defaultValue);
    GET_CSTR_RET(env, jKey,     key,     defaultValue);

    pthread_mutex_lock(&g_mutex);
    const char *val = get_value_internal(&g_store, section, key);
    char val_copy[MAX_VAL_LEN] = "";
    if (val) strncpy(val_copy, val, MAX_VAL_LEN - 1);
    pthread_mutex_unlock(&g_mutex);

    RELEASE_CSTR(env, jSection, section);
    RELEASE_CSTR(env, jKey,     key);

    if (!val) return defaultValue;

    char *endptr = NULL;
    errno = 0;
    double parsed = strtod(val_copy, &endptr);
    if (errno != 0 || endptr == val_copy || *endptr != '\0') {
        char msg[256];
        snprintf(msg, sizeof(msg),
                 "Cannot parse '%s' as double for key '%s'", val_copy, key);
        throw_config_exception(env, msg, errno, OP_GET_VALUE);
        return defaultValue;
    }
    return (jdouble)parsed;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * getBoolValue
 * Java_com_app_config_ConfigNative_getBoolValue
 * ═══════════════════════════════════════════════════════════════════════════ */
JNIEXPORT jboolean JNICALL
Java_com_app_config_ConfigNative_getBoolValue(JNIEnv *env, jobject thiz,
                                               jstring jSection, jstring jKey,
                                               jboolean defaultValue) {
    (void)thiz;

    GET_CSTR_RET(env, jSection, section, defaultValue);
    GET_CSTR_RET(env, jKey,     key,     defaultValue);

    pthread_mutex_lock(&g_mutex);
    const char *val = get_value_internal(&g_store, section, key);
    char val_copy[MAX_VAL_LEN] = "";
    if (val) strncpy(val_copy, val, MAX_VAL_LEN - 1);
    pthread_mutex_unlock(&g_mutex);

    RELEASE_CSTR(env, jSection, section);
    RELEASE_CSTR(env, jKey,     key);

    if (!val) return defaultValue;

    int b = parse_bool(val_copy);
    if (b < 0) {
        char msg[256];
        snprintf(msg, sizeof(msg),
                 "Cannot parse '%s' as boolean — accepted: "
                 "true/false/yes/no/on/off/1/0", val_copy);
        throw_config_exception(env, msg, 0, OP_GET_VALUE);
        return defaultValue;
    }
    return (jboolean)(b ? JNI_TRUE : JNI_FALSE);
}

/* ═══════════════════════════════════════════════════════════════════════════
 * getTotalKeyCount
 * Java_com_app_config_ConfigNative_getTotalKeyCount
 * ═══════════════════════════════════════════════════════════════════════════ */
JNIEXPORT jint JNICALL
Java_com_app_config_ConfigNative_getTotalKeyCount(JNIEnv *env, jobject thiz) {
    (void)env; (void)thiz;

    pthread_mutex_lock(&g_mutex);
    int total = 0;
    for (int i = 0; i < g_store.section_count; i++)
        total += g_store.sections[i].entry_count;
    pthread_mutex_unlock(&g_mutex);
    return (jint)total;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * getLoadedFilePath
 * Java_com_app_config_ConfigNative_getLoadedFilePath
 * ═══════════════════════════════════════════════════════════════════════════ */
JNIEXPORT jstring JNICALL
Java_com_app_config_ConfigNative_getLoadedFilePath(JNIEnv *env, jobject thiz) {
    (void)thiz;

    pthread_mutex_lock(&g_mutex);
    const char *path = g_store.loaded_path;
    jstring result = (path[0] != '\0') ? (*env)->NewStringUTF(env, path) : NULL;
    pthread_mutex_unlock(&g_mutex);
    return result;
}