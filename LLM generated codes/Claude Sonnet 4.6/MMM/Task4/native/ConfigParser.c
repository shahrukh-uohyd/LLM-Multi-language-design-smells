/*
 * ConfigParser.c
 *
 * JNI implementation for ConfigParser native methods.
 *
 * Methods implemented
 * ───────────────────
 *  Java_ConfigParser_parseKeyValue   – "key=value\n…" block parser
 *  Java_ConfigParser_parseIniSection – INI [section] + key=value parser
 *  Java_ConfigParser_parseIntList    – comma-separated integer parser
 *  Java_ConfigParser_parseDoubleList – comma-separated double parser
 *  Java_ConfigParser_parseFlagBlock  – "FLAG:0|1\n…" flag-block parser
 *
 * Build flags expected:
 *   -I${JAVA_HOME}/include  -I${JAVA_HOME}/include/<platform>
 */

#include <jni.h>
#include <ctype.h>
#include <math.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* ================================================================== */
/* Compile-time limits                                                  */
/* ================================================================== */
#define MAX_ENTRIES  256   /* max key-value pairs or flags per block   */
#define MAX_KEY_LEN  256   /* max key / flag-name length               */
#define MAX_VAL_LEN  512   /* max value length                         */
#define MAX_NUMS    1024   /* max numbers in a CSV list                 */

/* ================================================================== */
/* Internal helpers                                                     */
/* ================================================================== */

/** Trim leading + trailing ASCII whitespace in-place (modifies s). */
static char *trim(char *s) {
    while (*s && isspace((unsigned char)*s)) s++;
    char *end = s + strlen(s);
    while (end > s && isspace((unsigned char)*(end - 1))) end--;
    *end = '\0';
    return s;
}

/** Throw java.lang.OutOfMemoryError with a message. */
static void throwOOM(JNIEnv *env, const char *msg) {
    jclass c = (*env)->FindClass(env, "java/lang/OutOfMemoryError");
    if (c) (*env)->ThrowNew(env, c, msg);
}

/** Throw java.lang.IllegalArgumentException with a message. */
static void throwIAE(JNIEnv *env, const char *msg) {
    jclass c = (*env)->FindClass(env, "java/lang/IllegalArgumentException");
    if (c) (*env)->ThrowNew(env, c, msg);
}

/**
 * Build a Java object of the given class using a constructor whose
 * descriptor is desc.  Returns NULL (with pending exception) on error.
 */
static jobject newObject(JNIEnv   *env,
                         const char *className,
                         const char *ctorDesc,
                         ...) {
    jclass  cls  = (*env)->FindClass(env, className);
    if (!cls) return NULL;
    jmethodID mid = (*env)->GetMethodID(env, cls, "<init>", ctorDesc);
    if (!mid) return NULL;

    va_list ap;
    va_start(ap, ctorDesc);
    jobject obj = (*env)->NewObjectV(env, cls, mid, ap);
    va_end(ap);
    return obj;
}

/** Convert a C string array into a Java String[]. */
static jobjectArray cStringsToJava(JNIEnv *env, char **arr, int count) {
    jclass strCls = (*env)->FindClass(env, "java/lang/String");
    if (!strCls) return NULL;
    jobjectArray result = (*env)->NewObjectArray(env, count, strCls, NULL);
    if (!result) return NULL;
    for (int i = 0; i < count; i++) {
        jstring s = (*env)->NewStringUTF(env, arr[i] ? arr[i] : "");
        if (!s) return NULL;
        (*env)->SetObjectArrayElement(env, result, i, s);
        (*env)->DeleteLocalRef(env, s);
    }
    return result;
}

/* ================================================================== */
/* 1. parseKeyValue                                                     */
/*                                                                      */
/* Parses "key = value\n…" text.                                        */
/* Skips blank lines and lines whose first non-space char is '#'.       */
/* Returns a ConfigParser$KeyValueResult(String[] keys, String[] vals). */
/* ================================================================== */
JNIEXPORT jobject JNICALL
Java_ConfigParser_parseKeyValue(JNIEnv *env, jobject obj, jstring jraw)
{
    const char *raw = (*env)->GetStringUTFChars(env, jraw, NULL);
    if (!raw) return NULL;

    /* Working copies so we can mutate via strtok */
    char *buf = strdup(raw);
    (*env)->ReleaseStringUTFChars(env, jraw, raw);
    if (!buf) { throwOOM(env, "parseKeyValue: strdup"); return NULL; }

    char *keys[MAX_ENTRIES];
    char *vals[MAX_ENTRIES];
    int   count = 0;

    char *line = strtok(buf, "\n");
    while (line && count < MAX_ENTRIES) {
        char *t = line;
        while (*t && isspace((unsigned char)*t)) t++;

        /* Skip blank lines and comments */
        if (*t == '\0' || *t == '#') {
            line = strtok(NULL, "\n");
            continue;
        }

        /* Split on the first '=' */
        char *eq = strchr(t, '=');
        if (!eq) { line = strtok(NULL, "\n"); continue; }

        *eq = '\0';
        char *k = trim(t);
        char *v = trim(eq + 1);

        keys[count] = strdup(k);
        vals[count] = strdup(v);
        if (!keys[count] || !vals[count]) {
            free(buf);
            throwOOM(env, "parseKeyValue: strdup entry");
            return NULL;
        }
        count++;
        line = strtok(NULL, "\n");
    }
    free(buf);

    /* Build Java String arrays */
    jobjectArray jkeys = cStringsToJava(env, keys, count);
    jobjectArray jvals = cStringsToJava(env, vals, count);

    for (int i = 0; i < count; i++) { free(keys[i]); free(vals[i]); }

    if (!jkeys || !jvals) return NULL;

    /* Construct ConfigParser$KeyValueResult(String[], String[]) */
    return newObject(env,
        "ConfigParser$KeyValueResult",
        "([Ljava/lang/String;[Ljava/lang/String;)V",
        jkeys, jvals);
}

/* ================================================================== */
/* 2. parseIniSection                                                   */
/*                                                                      */
/* Parses one INI section block:                                        */
/*   [SectionName]                                                      */
/*   key1 = value1                                                      */
/*   …                                                                  */
/* Returns ConfigParser$IniSectionResult(String, String[], String[]).   */
/* ================================================================== */
JNIEXPORT jobject JNICALL
Java_ConfigParser_parseIniSection(JNIEnv *env, jobject obj, jstring jblock)
{
    const char *raw = (*env)->GetStringUTFChars(env, jblock, NULL);
    if (!raw) return NULL;

    char *buf = strdup(raw);
    (*env)->ReleaseStringUTFChars(env, jblock, raw);
    if (!buf) { throwOOM(env, "parseIniSection: strdup"); return NULL; }

    char  sectionName[MAX_KEY_LEN] = {0};
    char *keys[MAX_ENTRIES];
    char *vals[MAX_ENTRIES];
    int   count = 0;

    char *line = strtok(buf, "\n");
    while (line && count < MAX_ENTRIES) {
        char *t = line;
        while (*t && isspace((unsigned char)*t)) t++;

        if (*t == '\0' || *t == '#') {
            line = strtok(NULL, "\n"); continue;
        }

        if (*t == '[') {
            /* Extract section name between [ and ] */
            char *close = strchr(t, ']');
            if (close) {
                size_t namelen = (size_t)(close - t - 1);
                if (namelen >= MAX_KEY_LEN) namelen = MAX_KEY_LEN - 1;
                strncpy(sectionName, t + 1, namelen);
                sectionName[namelen] = '\0';
                /* Trim the section name */
                char *ts = trim(sectionName);
                if (ts != sectionName) memmove(sectionName, ts, strlen(ts) + 1);
            }
            line = strtok(NULL, "\n"); continue;
        }

        /* key = value line */
        char *eq = strchr(t, '=');
        if (!eq) { line = strtok(NULL, "\n"); continue; }

        *eq = '\0';
        char *k = trim(t);
        char *v = trim(eq + 1);

        keys[count] = strdup(k);
        vals[count] = strdup(v);
        if (!keys[count] || !vals[count]) {
            free(buf);
            throwOOM(env, "parseIniSection: strdup entry");
            return NULL;
        }
        count++;
        line = strtok(NULL, "\n");
    }
    free(buf);

    jstring      jname = (*env)->NewStringUTF(env, sectionName);
    jobjectArray jkeys = cStringsToJava(env, keys, count);
    jobjectArray jvals = cStringsToJava(env, vals, count);

    for (int i = 0; i < count; i++) { free(keys[i]); free(vals[i]); }

    if (!jname || !jkeys || !jvals) return NULL;

    return newObject(env,
        "ConfigParser$IniSectionResult",
        "(Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)V",
        jname, jkeys, jvals);
}

/* ================================================================== */
/* 3. parseIntList                                                      */
/*                                                                      */
/* Parses "n1, n2, n3, …" into a Java int[].                           */
/* Whitespace around commas is stripped; strtol used for parsing.       */
/* ================================================================== */
JNIEXPORT jintArray JNICALL
Java_ConfigParser_parseIntList(JNIEnv *env, jobject obj, jstring jcsv)
{
    const char *raw = (*env)->GetStringUTFChars(env, jcsv, NULL);
    if (!raw) return NULL;

    char *buf = strdup(raw);
    (*env)->ReleaseStringUTFChars(env, jcsv, raw);
    if (!buf) { throwOOM(env, "parseIntList: strdup"); return NULL; }

    jint  nums[MAX_NUMS];
    int   count = 0;

    char *token = strtok(buf, ",");
    while (token && count < MAX_NUMS) {
        char *t = trim(token);
        if (*t != '\0') {
            char *end;
            long v = strtol(t, &end, 10);
            if (end != t) {        /* at least one digit consumed */
                nums[count++] = (jint)v;
            }
        }
        token = strtok(NULL, ",");
    }
    free(buf);

    jintArray result = (*env)->NewIntArray(env, count);
    if (!result) return NULL;
    if (count > 0)
        (*env)->SetIntArrayRegion(env, result, 0, count, nums);
    return result;
}

/* ================================================================== */
/* 4. parseDoubleList                                                   */
/*                                                                      */
/* Parses "d1, d2, d3, …" into a Java double[].                        */
/* strtod is used for locale-independent parsing.                       */
/* ================================================================== */
JNIEXPORT jdoubleArray JNICALL
Java_ConfigParser_parseDoubleList(JNIEnv *env, jobject obj, jstring jcsv)
{
    const char *raw = (*env)->GetStringUTFChars(env, jcsv, NULL);
    if (!raw) return NULL;

    char *buf = strdup(raw);
    (*env)->ReleaseStringUTFChars(env, jcsv, raw);
    if (!buf) { throwOOM(env, "parseDoubleList: strdup"); return NULL; }

    jdouble nums[MAX_NUMS];
    int     count = 0;

    char *token = strtok(buf, ",");
    while (token && count < MAX_NUMS) {
        char *t = trim(token);
        if (*t != '\0') {
            char *end;
            double v = strtod(t, &end);
            if (end != t) {
                nums[count++] = (jdouble)v;
            }
        }
        token = strtok(NULL, ",");
    }
    free(buf);

    jdoubleArray result = (*env)->NewDoubleArray(env, count);
    if (!result) return NULL;
    if (count > 0)
        (*env)->SetDoubleArrayRegion(env, result, 0, count, nums);
    return result;
}

/* ================================================================== */
/* 5. parseFlagBlock                                                    */
/*                                                                      */
/* Parses "FLAG_NAME:0|1\n…" into a FlagResult(String[], boolean[]).   */
/* Lines starting with '#' are ignored.                                 */
/* "1" or "true" (case-insensitive) → true; everything else → false.   */
/* ================================================================== */
JNIEXPORT jobject JNICALL
Java_ConfigParser_parseFlagBlock(JNIEnv *env, jobject obj, jstring jblock)
{
    const char *raw = (*env)->GetStringUTFChars(env, jblock, NULL);
    if (!raw) return NULL;

    char *buf = strdup(raw);
    (*env)->ReleaseStringUTFChars(env, jblock, raw);
    if (!buf) { throwOOM(env, "parseFlagBlock: strdup"); return NULL; }

    char   *names[MAX_ENTRIES];
    jboolean states[MAX_ENTRIES];
    int     count = 0;

    char *line = strtok(buf, "\n");
    while (line && count < MAX_ENTRIES) {
        char *t = line;
        while (*t && isspace((unsigned char)*t)) t++;

        if (*t == '\0' || *t == '#') {
            line = strtok(NULL, "\n"); continue;
        }

        /* Split on ':' */
        char *colon = strchr(t, ':');
        if (!colon) { line = strtok(NULL, "\n"); continue; }

        *colon = '\0';
        char *name  = trim(t);
        char *state = trim(colon + 1);

        /* "1" or "true" (case-insensitive) → true */
        jboolean bval = (strcmp(state, "1") == 0 ||
#ifdef _MSC_VER
                         _stricmp(state, "true") == 0
#else
                         strcasecmp(state, "true") == 0
#endif
                        ) ? JNI_TRUE : JNI_FALSE;

        names[count]  = strdup(name);
        states[count] = bval;
        if (!names[count]) {
            free(buf);
            throwOOM(env, "parseFlagBlock: strdup name");
            return NULL;
        }
        count++;
        line = strtok(NULL, "\n");
    }
    free(buf);

    /* Build Java String[] for names */
    jobjectArray jnames = cStringsToJava(env, names, count);
    for (int i = 0; i < count; i++) free(names[i]);
    if (!jnames) return NULL;

    /* Build Java boolean[] for states */
    jbooleanArray jstates = (*env)->NewBooleanArray(env, count);
    if (!jstates) return NULL;
    if (count > 0)
        (*env)->SetBooleanArrayRegion(env, jstates, 0, count, states);

    return newObject(env,
        "ConfigParser$FlagResult",
        "([Ljava/lang/String;[Z)V",
        jnames, jstates);
}