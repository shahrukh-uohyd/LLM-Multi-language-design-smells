/**
 * text_processor.c
 *
 * JNI C implementation of the three-stage text-processing pipeline
 * declared in TextProcessor.java.
 *
 * ┌──────────────────────────────────────────────────────────────┐
 * │ Stage 1  Java_TextProcessor_nativeParse                      │
 * │          Tokenises the input string on whitespace.           │
 * │          Encodes tokens into the IR byte array.              │
 * │                                                              │
 * │ Stage 2  Java_TextProcessor_nativeProcess                    │
 * │          Decodes the IR, applies scoring rules:              │
 * │            R1  +SCORE_PER_ELEMENT  per token                 │
 * │            R2  +SCORE_LENGTH_BONUS per char over threshold   │
 * │          Appends the 4-byte aggregated score to a new IR.    │
 * │                                                              │
 * │ Stage 3  Java_TextProcessor_nativeGenerate                   │
 * │          Reads the aggregated score from the IR and returns  │
 * │          a formatted Java String result.                     │
 * └──────────────────────────────────────────────────────────────┘
 *
 * Error handling
 * ──────────────
 * Every failure path calls throw_processing_ex() which raises a Java
 * ProcessingException before returning a NULL / sentinel value.
 * JNI exception semantics require callers to return immediately after
 * a pending exception without making further JNI calls.
 */

#include "text_processor.h"
#include <jni.h>

#include <ctype.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

/* ═══════════════════════════════════════════════════════════════════════════
 *  Internal helper utilities
 * ═══════════════════════════════════════════════════════════════════════════ */

/* ── IR header read / write ─────────────────────────────────────────────── */

void ir_write_header(uint8_t* buf, uint8_t count, uint16_t total_len)
{
    buf[0] = IR_MAGIC_0;
    buf[1] = IR_MAGIC_1;
    buf[2] = IR_VERSION;
    buf[3] = count;
    /* total_len: little-endian */
    buf[4] = (uint8_t)(total_len & 0xFFu);
    buf[5] = (uint8_t)((total_len >> 8) & 0xFFu);
}

int ir_validate_header(const uint8_t* buf, size_t len)
{
    if (len < 6)              return 0;
    if (buf[0] != IR_MAGIC_0) return 0;
    if (buf[1] != IR_MAGIC_1) return 0;
    if (buf[2] != IR_VERSION) return 0;
    return 1;
}

/* ── JNI convenience: jbyteArray → C buffer ─────────────────────────────── */

/**
 * Copies a jbyteArray into a freshly malloc'd C buffer.
 * Caller must free() the returned pointer.
 * Sets *out_len to the number of bytes.
 * Returns NULL on allocation failure (no exception thrown here).
 */
static uint8_t* jbytearray_to_cbuf(JNIEnv* env, jbyteArray arr, size_t* out_len)
{
    jsize  len = (*env)->GetArrayLength(env, arr);
    jbyte* raw = (*env)->GetByteArrayElements(env, arr, NULL);
    if (!raw) return NULL;

    uint8_t* buf = (uint8_t*)malloc((size_t)len);
    if (buf)
        memcpy(buf, raw, (size_t)len);

    (*env)->ReleaseByteArrayElements(env, arr, raw, JNI_ABORT);
    *out_len = (size_t)len;
    return buf;
}

/* ── JNI convenience: C buffer → jbyteArray ────────────────────────────��── */

static jbyteArray cbuf_to_jbytearray(JNIEnv* env, const uint8_t* buf, size_t len)
{
    jbyteArray arr = (*env)->NewByteArray(env, (jsize)len);
    if (arr && len > 0)
        (*env)->SetByteArrayRegion(env, arr, 0, (jsize)len, (const jbyte*)buf);
    return arr;
}

/* ── Exception helper ────────────────────────────────────────────────────── */

/**
 * Raises a Java ProcessingException with the given stage field name
 * ("PARSE", "PROCESS", or "GENERATE") and message, then returns
 * {@code sentinel} so callers can write:
 *
 *     return throw_processing_ex(env, "PARSE", "bad input", NULL);
 */
static void* throw_processing_ex(JNIEnv*     env,
                                  const char* stage_field, /* "PARSE" etc. */
                                  const char* message)
{
    jclass ex_class = (*env)->FindClass(env, "ProcessingException");
    if (!ex_class) return NULL;  /* FindClass already threw */

    jclass stage_class = (*env)->FindClass(env, "ProcessingException$Stage");
    if (!stage_class) return NULL;

    jfieldID fid = (*env)->GetStaticFieldID(
            env, stage_class, stage_field, "LProcessingException$Stage;");
    if (!fid) return NULL;

    jobject stage_obj = (*env)->GetStaticObjectField(env, stage_class, fid);
    if (!stage_obj) return NULL;

    jmethodID ctor = (*env)->GetMethodID(
            env, ex_class, "<init>",
            "(LProcessingException$Stage;Ljava/lang/String;)V");
    if (!ctor) return NULL;

    jstring jmsg = (*env)->NewStringUTF(env, message);
    jobject ex   = (*env)->NewObject(env, ex_class, ctor, stage_obj, jmsg);
    if (ex) (*env)->Throw(env, (jthrowable)ex);

    return NULL;
}


/* ═══════════════════════════════════════════════════════════════════════════
 *  Stage 1 — nativeParse
 *
 *  Tokenises inputText on whitespace characters.
 *  Each token becomes one "element" in the IR.
 *
 *  IR layout after this stage:
 *    [magic0][magic1][version][count][total_len_lo][total_len_hi]
 *    [len0][...token0...][len1][...token1...]...
 * ═══════════════════════════════════════════════════════════════════════════ */
JNIEXPORT jbyteArray JNICALL
Java_TextProcessor_nativeParse(JNIEnv* env, jobject self, jstring jinput)
{
    (void)self;

    /* ── 1a. Obtain the input UTF-8 string from Java ─────────────────── */
    const char* input = (*env)->GetStringUTFChars(env, jinput, NULL);
    if (!input)
        return throw_processing_ex(env, "PARSE",
                                   "GetStringUTFChars returned NULL");

    size_t input_len = strlen(input);
    if (input_len == 0) {
        (*env)->ReleaseStringUTFChars(env, jinput, input);
        return throw_processing_ex(env, "PARSE", "Input text is empty");
    }

    /* ── 1b. Tokenise into a local array ─────────────────────────────── */
    /* We copy input so we can mutate it with strtok replacements. */
    char* work = (char*)malloc(input_len + 1);
    if (!work) {
        (*env)->ReleaseStringUTFChars(env, jinput, input);
        return throw_processing_ex(env, "PARSE", "Out of memory (work buffer)");
    }
    memcpy(work, input, input_len + 1);
    (*env)->ReleaseStringUTFChars(env, jinput, input);

    /* Collect token pointers and lengths */
    const char* tokens[MAX_ELEMENTS];
    uint8_t     tok_lens[MAX_ELEMENTS];
    uint32_t    count     = 0;
    uint32_t    total_len = 0;

    char* tok = strtok(work, " \t\r\n");
    while (tok != NULL) {
        if (count >= MAX_ELEMENTS) {
            free(work);
            return throw_processing_ex(env, "PARSE",
                                       "Input exceeds maximum element count (255)");
        }
        size_t tlen = strlen(tok);
        if (tlen > MAX_ELEM_LEN) {
            free(work);
            return throw_processing_ex(env, "PARSE",
                                       "A single element exceeds maximum length (255)");
        }
        tokens[count]   = tok;
        tok_lens[count] = (uint8_t)tlen;
        total_len      += (uint32_t)(1u + tlen); /* 1 byte length prefix + content */
        count++;
        tok = strtok(NULL, " \t\r\n");
    }

    if (count == 0) {
        free(work);
        return throw_processing_ex(env, "PARSE",
                                   "Input contains no parseable elements");
    }

    /* ── 1c. Serialise into IR buffer ────────────────────────────────── */
    /*  Header (6 bytes) + element block */
    size_t   ir_len = 6u + total_len;
    uint8_t* ir     = (uint8_t*)malloc(ir_len);
    if (!ir) {
        free(work);
        return throw_processing_ex(env, "PARSE", "Out of memory (IR buffer)");
    }

    ir_write_header(ir, (uint8_t)count, (uint16_t)total_len);

    uint8_t* cursor = ir + 6;
    for (uint32_t i = 0; i < count; i++) {
        *cursor++ = tok_lens[i];
        memcpy(cursor, tokens[i], tok_lens[i]);
        cursor += tok_lens[i];
    }

    free(work); /* tokens[] point into work — must free after memcpy */

    /* ── 1d. Return as jbyteArray ─────────────────────────────────────── */
    jbyteArray result = cbuf_to_jbytearray(env, ir, ir_len);
    free(ir);
    return result;
}


/* ═══════════════════════════════════════════════════════════════════════════
 *  Stage 2 — nativeProcess
 *
 *  Applies predefined scoring rules to the parsed elements:
 *    R1: every element contributes SCORE_PER_ELEMENT (10)
 *    R2: every character beyond LENGTH_BONUS_THRESHOLD (4) contributes
 *        SCORE_LENGTH_BONUS (5) additional points
 *
 *  Returns a new IR that is identical to the parse IR but with a
 *  4-byte little-endian aggregated score appended at the end.
 * ═══════════════════════════════════════════════════════════════════════════ */
JNIEXPORT jbyteArray JNICALL
Java_TextProcessor_nativeProcess(JNIEnv* env, jobject self, jbyteArray jparsed)
{
    (void)self;

    /* ── 2a. Copy the IR into a C buffer ─────────────────────────────── */
    size_t   ir_len = 0;
    uint8_t* ir     = jbytearray_to_cbuf(env, jparsed, &ir_len);
    if (!ir)
        return throw_processing_ex(env, "PROCESS",
                                   "Failed to read parsed IR from Java");

    /* ── 2b. Validate header ─────────────────────────────────────────── */
    if (!ir_validate_header(ir, ir_len)) {
        free(ir);
        return throw_processing_ex(env, "PROCESS",
                                   "Parsed IR has invalid or corrupt header");
    }

    uint8_t  count     = ir[3];
    /* total_len (little-endian) is in bytes 4-5 — not needed for scoring */

    /* ── 2c. Walk the element block and apply rules ───────────────────── */
    uint32_t score  = 0;
    size_t   offset = 6; /* skip 6-byte header */

    for (uint8_t i = 0; i < count; i++) {
        if (offset >= ir_len) {
            free(ir);
            return throw_processing_ex(env, "PROCESS",
                                       "IR is truncated while reading elements");
        }

        uint8_t elem_len = ir[offset];
        offset += 1u + elem_len;

        if (offset > ir_len) {
            free(ir);
            return throw_processing_ex(env, "PROCESS",
                                       "IR element extends beyond buffer boundary");
        }

        /* R1: base score per element */
        score += SCORE_PER_ELEMENT;

        /* R2: length bonus for elements exceeding the threshold */
        if (elem_len > LENGTH_BONUS_THRESHOLD)
            score += (uint32_t)(elem_len - LENGTH_BONUS_THRESHOLD)
                     * SCORE_LENGTH_BONUS;
    }

    /* ── 2d. Build processed IR = original IR + 4-byte score ─────────── */
    size_t   out_len = ir_len + 4u;
    uint8_t* out     = (uint8_t*)malloc(out_len);
    if (!out) {
        free(ir);
        return throw_processing_ex(env, "PROCESS",
                                   "Out of memory (processed IR buffer)");
    }

    memcpy(out, ir, ir_len);
    free(ir);

    /* Append score in little-endian order */
    out[ir_len + 0] = (uint8_t)(score & 0xFFu);
    out[ir_len + 1] = (uint8_t)((score >>  8) & 0xFFu);
    out[ir_len + 2] = (uint8_t)((score >> 16) & 0xFFu);
    out[ir_len + 3] = (uint8_t)((score >> 24) & 0xFFu);

    /* ── 2e. Return as jbyteArray ─────────────────────────────────────── */
    jbyteArray result = cbuf_to_jbytearray(env, out, out_len);
    free(out);
    return result;
}


/* ═══════════════════════════════════════════════════════════════════════════
 *  Stage 3 — nativeGenerate
 *
 *  Reads the 4-byte aggregated score appended by nativeProcess and
 *  formats the final output string:
 *
 *    "elements=<N>;score=<S>"
 *
 *  This string is returned to Java as a jstring.
 * ═══════════════════════════════════════════════════════════════════════════ */
JNIEXPORT jstring JNICALL
Java_TextProcessor_nativeGenerate(JNIEnv* env, jobject self, jbyteArray jprocessed)
{
    (void)self;

    /* ── 3a. Copy processed IR ───────────────────────────────────────── */
    size_t   ir_len = 0;
    uint8_t* ir     = jbytearray_to_cbuf(env, jprocessed, &ir_len);
    if (!ir)
        return throw_processing_ex(env, "GENERATE",
                                   "Failed to read processed IR from Java");

    /* Minimum valid size: 6-byte header + at least 4-byte score trailer */
    if (!ir_validate_header(ir, ir_len) || ir_len < 10u) {
        free(ir);
        return throw_processing_ex(env, "GENERATE",
                                   "Processed IR is too short or has corrupt header");
    }

    /* ── 3b. Extract element count from header ───────────────────────── */
    uint8_t count = ir[3];

    /* ── 3c. Read the 4-byte little-endian score at the end ──────────── */
    uint32_t score =
        ((uint32_t)ir[ir_len - 4])        |
        ((uint32_t)ir[ir_len - 3] <<  8)  |
        ((uint32_t)ir[ir_len - 2] << 16)  |
        ((uint32_t)ir[ir_len - 1] << 24);

    free(ir);

    /* ── 3d. Format output string and return to Java ─────────────────── */
    char buf[64];
    snprintf(buf, sizeof(buf), "elements=%u;score=%u",
             (unsigned)count, (unsigned)score);

    return (*env)->NewStringUTF(env, buf);
}