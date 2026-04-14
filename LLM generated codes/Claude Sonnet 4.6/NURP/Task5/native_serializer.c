/*
 * native_serializer.c
 *
 * Full native implementation of every serialization, deserialization,
 * checksum-validation, and RLE-compression routine declared in
 * NativeSerializer.java.
 *
 * ── Wire format (little-endian) ──────────────────────────────────────────
 *
 *  Single record:
 *  ┌──────────┬──────────┬──────────┬────────┬────────────┬──────────────┐
 *  │ MAGIC(4) │  id(8)   │ score(8) │ act(1) │ name_len(4)│  name(var)  │
 *  ├──────────┴──────────┴──────────┴────────┴────────────┴──────────────┤
 *  │ tag_count(4) │  [tag_len(4) + tag_bytes(var)] × tag_count           │
 *  ├──────────────┬────────────────────────────────────────┬─────────────┤
 *  │ payload_len(4)│            payload bytes              │ checksum(4) │
 *  └──────────────┴────────────────────────────────────────┴─────────────┘
 *
 *  Batch buffer:
 *  ┌────────────────┬───────────────────────────────────────────────────┐
 *  │ record_count(4)│  record_0  │  record_1  │  …  │  record_n-1      │
 *  └────────────────┴───────────────────────────────────────────────────┘
 *  Each embedded record has its own magic and checksum.
 *
 *  RLE compressed block:
 *  ┌──────────────────┬──────────────────────────────────────────────────┐
 *  │ RLE_MAGIC(4)     │  original_len(4)  │  [count(2)+byte(1)] × runs  │
 *  └──────────────────┴──────────────────────────────────────────────────┘
 *
 * Compile (Linux):
 *   gcc -O2 -shared -fPIC \
 *       -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
 *       -o ../native_libs/libnative_serializer.so \
 *       native_serializer.c
 *
 * Compile (macOS):
 *   gcc -O2 -dynamiclib \
 *       -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin" \
 *       -o ../native_libs/libnative_serializer.dylib \
 *       native_serializer.c
 *
 * Compile (Windows, MinGW):
 *   gcc -O2 -shared \
 *       -I"%JAVA_HOME%/include" -I"%JAVA_HOME%/include/win32" \
 *       -o ../native_libs/native_serializer.dll \
 *       native_serializer.c
 */

#include "native_serializer.h"
#include <stdlib.h>
#include <string.h>
#include <stdint.h>

/* ── Format constants ────────────────────────────────────────────────────── */

#define MAGIC_RECORD  UINT32_C(0x4E535231)   /* "NSR1" */
#define MAGIC_BATCH   UINT32_C(0x4E534231)   /* "NSB1" */
#define MAGIC_RLE     UINT32_C(0x524C4531)   /* "RLE1" */

/* Overhead: magic(4)+id(8)+score(8)+active(1)+name_len(4)+tag_count(4)+
 *           payload_len(4)+checksum(4) = 37 bytes fixed minimum           */
#define RECORD_FIXED_OVERHEAD 37

/* ═══════════════════════════════════════════════════════════════════════════
 * Growable byte buffer
 * ═══════════════════════════════════════════════════════════════════════════*/

typedef struct {
    uint8_t *data;
    size_t   size;     /* bytes written */
    size_t   capacity;
} ByteBuffer;

static int buf_init(ByteBuffer *b, size_t initial) {
    b->data = (uint8_t *)malloc(initial);
    if (!b->data) return 0;
    b->size = 0;
    b->capacity = initial;
    return 1;
}

static void buf_free(ByteBuffer *b) {
    free(b->data);
    b->data = NULL;
    b->size = b->capacity = 0;
}

static int buf_ensure(ByteBuffer *b, size_t extra) {
    if (b->size + extra <= b->capacity) return 1;
    size_t newcap = b->capacity * 2 + extra;
    uint8_t *tmp = (uint8_t *)realloc(b->data, newcap);
    if (!tmp) return 0;
    b->data = tmp;
    b->capacity = newcap;
    return 1;
}

/* ── Typed writers (little-endian) ─────────────────────────────────────── */

static int buf_write_u8(ByteBuffer *b, uint8_t v) {
    if (!buf_ensure(b, 1)) return 0;
    b->data[b->size++] = v;
    return 1;
}

static int buf_write_u16le(ByteBuffer *b, uint16_t v) {
    if (!buf_ensure(b, 2)) return 0;
    b->data[b->size++] = (uint8_t)(v);
    b->data[b->size++] = (uint8_t)(v >> 8);
    return 1;
}

static int buf_write_u32le(ByteBuffer *b, uint32_t v) {
    if (!buf_ensure(b, 4)) return 0;
    b->data[b->size++] = (uint8_t)(v);
    b->data[b->size++] = (uint8_t)(v >>  8);
    b->data[b->size++] = (uint8_t)(v >> 16);
    b->data[b->size++] = (uint8_t)(v >> 24);
    return 1;
}

static int buf_write_u64le(ByteBuffer *b, uint64_t v) {
    if (!buf_ensure(b, 8)) return 0;
    for (int i = 0; i < 8; i++) {
        b->data[b->size++] = (uint8_t)(v >> (8 * i));
    }
    return 1;
}

static int buf_write_f64le(ByteBuffer *b, double v) {
    uint64_t bits;
    memcpy(&bits, &v, 8);
    return buf_write_u64le(b, bits);
}

static int buf_write_bytes(ByteBuffer *b, const uint8_t *src, size_t len) {
    if (!buf_ensure(b, len)) return 0;
    memcpy(b->data + b->size, src, len);
    b->size += len;
    return 1;
}

/* ── Typed readers (little-endian) ─────────────────────────────────────── */

typedef struct {
    const uint8_t *data;
    size_t         size;   /* total bytes available */
    size_t         pos;    /* current read cursor   */
} ByteReader;

static void reader_init(ByteReader *r, const uint8_t *data, size_t size) {
    r->data = data; r->size = size; r->pos = 0;
}

static int reader_can_read(const ByteReader *r, size_t n) {
    return (r->pos + n) <= r->size;
}

static uint8_t reader_u8(ByteReader *r) {
    return r->data[r->pos++];
}

static uint16_t reader_u16le(ByteReader *r) {
    uint16_t v = (uint16_t)r->data[r->pos]
               | ((uint16_t)r->data[r->pos + 1] << 8);
    r->pos += 2;
    return v;
}

static uint32_t reader_u32le(ByteReader *r) {
    uint32_t v = (uint32_t)r->data[r->pos]
               | ((uint32_t)r->data[r->pos+1] <<  8)
               | ((uint32_t)r->data[r->pos+2] << 16)
               | ((uint32_t)r->data[r->pos+3] << 24);
    r->pos += 4;
    return v;
}

static uint64_t reader_u64le(ByteReader *r) {
    uint64_t v = 0;
    for (int i = 0; i < 8; i++)
        v |= ((uint64_t)r->data[r->pos + i]) << (8 * i);
    r->pos += 8;
    return v;
}

static double reader_f64le(ByteReader *r) {
    uint64_t bits = reader_u64le(r);
    double v;
    memcpy(&v, &bits, 8);
    return v;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * XOR checksum (covers all bytes before the 4-byte checksum field)
 * ═══════════════════════════════════════════════════════════════════════════*/

static uint32_t compute_xor_checksum(const uint8_t *data, size_t len) {
    uint32_t csum = 0xDEADBEEF;
    for (size_t i = 0; i < len; i++) {
        csum ^= ((uint32_t)data[i] << ((i % 4) * 8));
        /* rotate left by 3 to spread bits */
        csum = (csum << 3) | (csum >> 29);
    }
    return csum;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * Core encode / decode of a single record (internal helpers)
 * ═══════════════════════════════════════════════════════════════════���═══════*/

/*
 * encode_record_to_buf — writes one complete record into buf.
 * tag_strings: array of jstring (may be NULL if tag_count == 0)
 * payload_data / payload_len: raw bytes
 */
static int encode_record_to_buf(
        JNIEnv *env, ByteBuffer *buf,
        jlong id, jstring jname, jdouble score, jboolean active,
        jobjectArray jtags, int tag_count,
        const uint8_t *payload_data, uint32_t payload_len)
{
    /* Magic */
    if (!buf_write_u32le(buf, MAGIC_RECORD)) return 0;

    /* id (int64) */
    if (!buf_write_u64le(buf, (uint64_t)(int64_t)id)) return 0;

    /* score (float64) */
    if (!buf_write_f64le(buf, (double)score)) return 0;

    /* active (1 byte) */
    if (!buf_write_u8(buf, active ? 1 : 0)) return 0;

    /* name */
    const char *name_utf = (*env)->GetStringUTFChars(env, jname, NULL);
    if (!name_utf) return 0;
    uint32_t name_len = (uint32_t)strlen(name_utf);
    int ok = buf_write_u32le(buf, name_len) &&
             buf_write_bytes(buf, (const uint8_t *)name_utf, name_len);
    (*env)->ReleaseStringUTFChars(env, jname, name_utf);
    if (!ok) return 0;

    /* tags */
    if (!buf_write_u32le(buf, (uint32_t)tag_count)) return 0;
    for (int i = 0; i < tag_count; i++) {
        jstring jtag = (jstring)(*env)->GetObjectArrayElement(env, jtags, i);
        const char *tag_utf = (*env)->GetStringUTFChars(env, jtag, NULL);
        uint32_t tag_len = (uint32_t)strlen(tag_utf);
        ok = buf_write_u32le(buf, tag_len) &&
             buf_write_bytes(buf, (const uint8_t *)tag_utf, tag_len);
        (*env)->ReleaseStringUTFChars(env, jtag, tag_utf);
        (*env)->DeleteLocalRef(env, jtag);
        if (!ok) return 0;
    }

    /* payload */
    if (!buf_write_u32le(buf, payload_len)) return 0;
    if (payload_len > 0)
        if (!buf_write_bytes(buf, payload_data, payload_len)) return 0;

    /* checksum over everything written so far */
    uint32_t csum = compute_xor_checksum(buf->data, buf->size);
    return buf_write_u32le(buf, csum);
}

/*
 * decode_record_from_reader — reads one complete record from reader.
 * On success, populates the output pointers and returns 1.
 * The caller is responsible for releasing JNI references.
 *
 * Outputs:
 *   out_id, out_name (NewStringUTF), out_score, out_active,
 *   out_tags (NewObjectArray of jstring), out_payload (NewByteArray),
 *   out_consumed (bytes read including checksum)
 */
static int decode_record_from_reader(
        JNIEnv *env, ByteReader *r,
        jlong *out_id, jstring *out_name, jdouble *out_score,
        jboolean *out_active, jobjectArray *out_tags,
        jbyteArray *out_payload, jint *out_consumed)
{
    size_t start_pos = r->pos;

    /* Magic */
    if (!reader_can_read(r, 4)) return 0;
    uint32_t magic = reader_u32le(r);
    if (magic != MAGIC_RECORD) return 0;

    /* id */
    if (!reader_can_read(r, 8)) return 0;
    *out_id = (jlong)(int64_t)reader_u64le(r);

    /* score */
    if (!reader_can_read(r, 8)) return 0;
    *out_score = (jdouble)reader_f64le(r);

    /* active */
    if (!reader_can_read(r, 1)) return 0;
    *out_active = reader_u8(r) ? JNI_TRUE : JNI_FALSE;

    /* name */
    if (!reader_can_read(r, 4)) return 0;
    uint32_t name_len = reader_u32le(r);
    if (!reader_can_read(r, name_len)) return 0;
    char *name_buf = (char *)malloc(name_len + 1);
    if (!name_buf) return 0;
    memcpy(name_buf, r->data + r->pos, name_len);
    name_buf[name_len] = '\0';
    r->pos += name_len;
    *out_name = (*env)->NewStringUTF(env, name_buf);
    free(name_buf);
    if (!*out_name) return 0;

    /* tags */
    if (!reader_can_read(r, 4)) return 0;
    uint32_t tag_count = reader_u32le(r);
    jclass str_class = (*env)->FindClass(env, "java/lang/String");
    *out_tags = (*env)->NewObjectArray(env, (jsize)tag_count, str_class, NULL);
    if (!*out_tags) return 0;

    for (uint32_t i = 0; i < tag_count; i++) {
        if (!reader_can_read(r, 4)) return 0;
        uint32_t tlen = reader_u32le(r);
        if (!reader_can_read(r, tlen)) return 0;
        char *tbuf = (char *)malloc(tlen + 1);
        if (!tbuf) return 0;
        memcpy(tbuf, r->data + r->pos, tlen);
        tbuf[tlen] = '\0';
        r->pos += tlen;
        jstring jtag = (*env)->NewStringUTF(env, tbuf);
        free(tbuf);
        (*env)->SetObjectArrayElement(env, *out_tags, (jsize)i, jtag);
        (*env)->DeleteLocalRef(env, jtag);
    }

    /* payload */
    if (!reader_can_read(r, 4)) return 0;
    uint32_t payload_len = reader_u32le(r);
    if (!reader_can_read(r, payload_len)) return 0;
    *out_payload = (*env)->NewByteArray(env, (jsize)payload_len);
    if (!*out_payload) return 0;
    if (payload_len > 0)
        (*env)->SetByteArrayRegion(env, *out_payload, 0, (jsize)payload_len,
                                   (const jbyte *)(r->data + r->pos));
    r->pos += payload_len;

    /* checksum — verify, then skip */
    if (!reader_can_read(r, 4)) return 0;
    uint32_t stored_csum   = reader_u32le(r);
    uint32_t computed_csum = compute_xor_checksum(r->data + start_pos,
                                                  r->pos - 4 - start_pos);
    if (stored_csum != computed_csum) return 0;   /* checksum mismatch */

    *out_consumed = (jint)(r->pos - start_pos);
    return 1;
}

/* ═══════════════════════════════════════════════════════════════════════════
 * JNI implementations
 * ═══════════════════════════════════════════════════════════════════════════*/

/* ── nativeSerialize ────────────────────────────────────────────────────── */
JNIEXPORT jbyteArray JNICALL
Java_com_serializer_NativeSerializer_nativeSerialize(
        JNIEnv *env, jobject obj,
        jlong id, jstring jname, jdouble score, jboolean active,
        jobjectArray jtags, jbyteArray jpayload)
{
    (void)obj;

    /* Pin payload bytes */
    jsize payload_len = (*env)->GetArrayLength(env, jpayload);
    jbyte *payload_data = (payload_len > 0)
        ? (*env)->GetByteArrayElements(env, jpayload, NULL)
        : NULL;

    jsize tag_count = jtags ? (*env)->GetArrayLength(env, jtags) : 0;

    ByteBuffer buf;
    if (!buf_init(&buf, 128 + payload_len)) {
        if (payload_data) (*env)->ReleaseByteArrayElements(env, jpayload, payload_data, JNI_ABORT);
        return NULL;
    }

    int ok = encode_record_to_buf(env, &buf, id, jname, score, active,
                                  jtags, (int)tag_count,
                                  (const uint8_t *)payload_data,
                                  (uint32_t)payload_len);

    if (payload_data)
        (*env)->ReleaseByteArrayElements(env, jpayload, payload_data, JNI_ABORT);

    if (!ok) { buf_free(&buf); return NULL; }

    jbyteArray result = (*env)->NewByteArray(env, (jsize)buf.size);
    if (result)
        (*env)->SetByteArrayRegion(env, result, 0, (jsize)buf.size, (jbyte *)buf.data);

    buf_free(&buf);
    return result;
}

/* ── nativeDeserialize ──────────────────────────────────────────────────── */
JNIEXPORT jobjectArray JNICALL
Java_com_serializer_NativeSerializer_nativeDeserialize(
        JNIEnv *env, jobject obj,
        jbyteArray jdata, jint offset, jint length)
{
    (void)obj;

    jbyte *raw = (*env)->GetByteArrayElements(env, jdata, NULL);
    if (!raw) return NULL;

    ByteReader reader;
    reader_init(&reader, (const uint8_t *)raw + offset, (size_t)length);

    jlong      out_id;
    jstring    out_name    = NULL;
    jdouble    out_score;
    jboolean   out_active;
    jobjectArray out_tags  = NULL;
    jbyteArray out_payload = NULL;
    jint       out_consumed;

    int ok = decode_record_from_reader(env, &reader,
                 &out_id, &out_name, &out_score, &out_active,
                 &out_tags, &out_payload, &out_consumed);

    (*env)->ReleaseByteArrayElements(env, jdata, raw, JNI_ABORT);

    if (!ok) return NULL;

    /* Return Object[7]: {Long, String, Double, Boolean, String[], byte[], Integer} */
    jclass obj_class = (*env)->FindClass(env, "java/lang/Object");
    jobjectArray result = (*env)->NewObjectArray(env, 7, obj_class, NULL);
    if (!result) return NULL;

    /* Box primitives */
    jclass long_c    = (*env)->FindClass(env, "java/lang/Long");
    jclass double_c  = (*env)->FindClass(env, "java/lang/Double");
    jclass bool_c    = (*env)->FindClass(env, "java/lang/Boolean");
    jclass int_c     = (*env)->FindClass(env, "java/lang/Integer");

    jobject boxed_id     = (*env)->NewObject(env, long_c,
                               (*env)->GetMethodID(env, long_c,   "<init>", "(J)V"), out_id);
    jobject boxed_score  = (*env)->NewObject(env, double_c,
                               (*env)->GetMethodID(env, double_c, "<init>", "(D)V"), out_score);
    jobject boxed_active = (*env)->NewObject(env, bool_c,
                               (*env)->GetMethodID(env, bool_c,   "<init>", "(Z)V"), out_active);
    jobject boxed_cons   = (*env)->NewObject(env, int_c,
                               (*env)->GetMethodID(env, int_c,    "<init>", "(I)V"), out_consumed);

    (*env)->SetObjectArrayElement(env, result, 0, boxed_id);
    (*env)->SetObjectArrayElement(env, result, 1, out_name);
    (*env)->SetObjectArrayElement(env, result, 2, boxed_score);
    (*env)->SetObjectArrayElement(env, result, 3, boxed_active);
    (*env)->SetObjectArrayElement(env, result, 4, out_tags);
    (*env)->SetObjectArrayElement(env, result, 5, out_payload);
    (*env)->SetObjectArrayElement(env, result, 6, boxed_cons);

    return result;
}

/* ── nativeSerializeBatch ───────────────────────────────────────────────── */
JNIEXPORT jbyteArray JNICALL
Java_com_serializer_NativeSerializer_nativeSerializeBatch(
        JNIEnv *env, jobject obj,
        jlongArray jids, jobjectArray jnames, jdoubleArray jscores,
        jbooleanArray jactives, jobjectArray jtags_arr, jobjectArray jpayloads)
{
    (void)obj;

    jsize count = (*env)->GetArrayLength(env, jids);

    jlong    *ids     = (*env)->GetLongArrayElements   (env, jids,     NULL);
    jdouble  *scores  = (*env)->GetDoubleArrayElements (env, jscores,  NULL);
    jboolean *actives = (*env)->GetBooleanArrayElements(env, jactives, NULL);

    ByteBuffer buf;
    if (!buf_init(&buf, 256)) goto cleanup_fail;

    /* Batch magic + record count */
    if (!buf_write_u32le(&buf, MAGIC_BATCH)) goto cleanup_fail;
    if (!buf_write_u32le(&buf, (uint32_t)count)) goto cleanup_fail;

    for (jsize i = 0; i < count; i++) {
        jstring    jname    = (jstring)    (*env)->GetObjectArrayElement(env, jnames,     i);
        jobjectArray jtags  = (jobjectArray)(*env)->GetObjectArrayElement(env, jtags_arr, i);
        jbyteArray  jpay    = (jbyteArray)  (*env)->GetObjectArrayElement(env, jpayloads, i);

        jsize pay_len = (*env)->GetArrayLength(env, jpay);
        jbyte *pay_data = (pay_len > 0) ? (*env)->GetByteArrayElements(env, jpay, NULL) : NULL;
        jsize tag_cnt   = jtags ? (*env)->GetArrayLength(env, jtags) : 0;

        int ok = encode_record_to_buf(env, &buf,
                     ids[i], jname, scores[i], actives[i],
                     jtags, (int)tag_cnt,
                     (const uint8_t *)pay_data, (uint32_t)pay_len);

        if (pay_data) (*env)->ReleaseByteArrayElements(env, jpay, pay_data, JNI_ABORT);
        (*env)->DeleteLocalRef(env, jname);
        (*env)->DeleteLocalRef(env, jtags);
        (*env)->DeleteLocalRef(env, jpay);

        if (!ok) goto cleanup_fail;
    }

    {
        jbyteArray result = (*env)->NewByteArray(env, (jsize)buf.size);
        if (result)
            (*env)->SetByteArrayRegion(env, result, 0, (jsize)buf.size, (jbyte *)buf.data);
        buf_free(&buf);
        (*env)->ReleaseLongArrayElements   (env, jids,     ids,     JNI_ABORT);
        (*env)->ReleaseDoubleArrayElements (env, jscores,  scores,  JNI_ABORT);
        (*env)->ReleaseBooleanArrayElements(env, jactives, actives, JNI_ABORT);
        return result;
    }

cleanup_fail:
    buf_free(&buf);
    if (ids)     (*env)->ReleaseLongArrayElements   (env, jids,     ids,     JNI_ABORT);
    if (scores)  (*env)->ReleaseDoubleArrayElements (env, jscores,  scores,  JNI_ABORT);
    if (actives) (*env)->ReleaseBooleanArrayElements(env, jactives, actives, JNI_ABORT);
    return NULL;
}

/* ── nativeDeserializeBatch ─────────────────────────────────────────────── */
JNIEXPORT jobjectArray JNICALL
Java_com_serializer_NativeSerializer_nativeDeserializeBatch(
        JNIEnv *env, jobject obj,
        jbyteArray jdata, jint length)
{
    (void)obj;

    jbyte *raw = (*env)->GetByteArrayElements(env, jdata, NULL);
    if (!raw) return NULL;

    ByteReader r;
    reader_init(&r, (const uint8_t *)raw, (size_t)length);

    /* Batch magic */
    if (!reader_can_read(&r, 4)) goto fail;
    if (reader_u32le(&r) != MAGIC_BATCH) goto fail;
    if (!reader_can_read(&r, 4)) goto fail;
    uint32_t count = reader_u32le(&r);

    /* Parallel output arrays */
    jlongArray    out_ids;
    jobjectArray  out_names;
    jdoubleArray  out_scores;
    jbooleanArray out_actives;
    jobjectArray  out_tags_arr;
    jobjectArray  out_payloads;

    out_ids     = (*env)->NewLongArray   (env, (jsize)count);
    out_scores  = (*env)->NewDoubleArray (env, (jsize)count);
    out_actives = (*env)->NewBooleanArray(env, (jsize)count);

    jclass str_cls  = (*env)->FindClass(env, "java/lang/String");
    jclass barr_cls = (*env)->FindClass(env, "[B");
    jclass oarr_cls = (*env)->FindClass(env, "[Ljava/lang/String;");

    out_names     = (*env)->NewObjectArray(env, (jsize)count, str_cls,  NULL);
    out_tags_arr  = (*env)->NewObjectArray(env, (jsize)count, oarr_cls, NULL);
    out_payloads  = (*env)->NewObjectArray(env, (jsize)count, barr_cls, NULL);

    for (uint32_t i = 0; i < count; i++) {
        jlong id; jstring name; jdouble score; jboolean active;
        jobjectArray tags; jbyteArray payload; jint consumed;

        if (!decode_record_from_reader(env, &r,
                &id, &name, &score, &active, &tags, &payload, &consumed))
            goto fail;

        (*env)->SetLongArrayRegion   (env, out_ids,     (jsize)i, 1, &id);
        (*env)->SetDoubleArrayRegion (env, out_scores,  (jsize)i, 1, &score);
        (*env)->SetBooleanArrayRegion(env, out_actives, (jsize)i, 1, &active);
        (*env)->SetObjectArrayElement(env, out_names,     (jsize)i, name);
        (*env)->SetObjectArrayElement(env, out_tags_arr,  (jsize)i, tags);
        (*env)->SetObjectArrayElement(env, out_payloads,  (jsize)i, payload);

        (*env)->DeleteLocalRef(env, name);
        (*env)->DeleteLocalRef(env, tags);
        (*env)->DeleteLocalRef(env, payload);
    }

    (*env)->ReleaseByteArrayElements(env, jdata, raw, JNI_ABORT);

    {
        jclass obj_cls = (*env)->FindClass(env, "java/lang/Object");
        jobjectArray result = (*env)->NewObjectArray(env, 6, obj_cls, NULL);
        (*env)->SetObjectArrayElement(env, result, 0, out_ids);
        (*env)->SetObjectArrayElement(env, result, 1, out_names);
        (*env)->SetObjectArrayElement(env, result, 2, out_scores);
        (*env)->SetObjectArrayElement(env, result, 3, out_actives);
        (*env)->SetObjectArrayElement(env, result, 4, out_tags_arr);
        (*env)->SetObjectArrayElement(env, result, 5, out_payloads);
        return result;
    }

fail:
    (*env)->ReleaseByteArrayElements(env, jdata, raw, JNI_ABORT);
    return NULL;
}

/* ── nativeValidateChecksum ─────────────────────────────────────────────── */
JNIEXPORT jboolean JNICALL
Java_com_serializer_NativeSerializer_nativeValidateChecksum(
        JNIEnv *env, jobject obj,
        jbyteArray jdata, jint length)
{
    (void)obj;
    if (length < 8) return JNI_FALSE;

    jbyte *raw = (*env)->GetByteArrayElements(env, jdata, NULL);
    if (!raw) return JNI_FALSE;

    /* The last 4 bytes are the stored checksum; compute over the rest */
    size_t body_len = (size_t)length - 4;
    uint32_t stored   = (uint32_t)(uint8_t)raw[length - 4]
                      | ((uint32_t)(uint8_t)raw[length - 3] <<  8)
                      | ((uint32_t)(uint8_t)raw[length - 2] << 16)
                      | ((uint32_t)(uint8_t)raw[length - 1] << 24);
    uint32_t computed = compute_xor_checksum((const uint8_t *)raw, body_len);

    (*env)->ReleaseByteArrayElements(env, jdata, raw, JNI_ABORT);
    return (stored == computed) ? JNI_TRUE : JNI_FALSE;
}

/* ═���═════════════════════════════════════════════════════════════════════════
 * RLE Compression / Decompression
 *
 * Format: [MAGIC_RLE(4)] [original_len(4)] [runs...]
 *   Each run: [count(2, little-endian)] [byte_value(1)]
 *   Maximum run length per entry: 65535
 * ═══════════════════════════════════════════════════════════════════════════*/

JNIEXPORT jbyteArray JNICALL
Java_com_serializer_NativeSerializer_nativeCompressPayload(
        JNIEnv *env, jobject obj,
        jbyteArray jdata, jint length)
{
    (void)obj;
    if (length == 0) {
        jbyteArray r = (*env)->NewByteArray(env, 8);
        /* Write magic + original_len=0 */
        uint8_t hdr[8] = {
            (uint8_t)(MAGIC_RLE),       (uint8_t)(MAGIC_RLE >> 8),
            (uint8_t)(MAGIC_RLE >> 16), (uint8_t)(MAGIC_RLE >> 24),
            0, 0, 0, 0
        };
        (*env)->SetByteArrayRegion(env, r, 0, 8, (jbyte *)hdr);
        return r;
    }

    jbyte *raw = (*env)->GetByteArrayElements(env, jdata, NULL);
    if (!raw) return NULL;

    ByteBuffer buf;
    /* Worst case: every byte is unique → 3 bytes per input byte + 8 header */
    if (!buf_init(&buf, 8 + (size_t)length * 3)) {
        (*env)->ReleaseByteArrayElements(env, jdata, raw, JNI_ABORT);
        return NULL;
    }

    buf_write_u32le(&buf, MAGIC_RLE);
    buf_write_u32le(&buf, (uint32_t)length);   /* original length */

    size_t i = 0;
    while (i < (size_t)length) {
        uint8_t val = (uint8_t)raw[i];
        uint32_t run = 1;
        while (i + run < (size_t)length &&
               (uint8_t)raw[i + run] == val &&
               run < 65535)
            run++;
        buf_write_u16le(&buf, (uint16_t)run);
        buf_write_u8(&buf, val);
        i += run;
    }

    (*env)->ReleaseByteArrayElements(env, jdata, raw, JNI_ABORT);

    jbyteArray result = (*env)->NewByteArray(env, (jsize)buf.size);
    if (result)
        (*env)->SetByteArrayRegion(env, result, 0, (jsize)buf.size, (jbyte *)buf.data);
    buf_free(&buf);
    return result;
}

JNIEXPORT jbyteArray JNICALL
Java_com_serializer_NativeSerializer_nativeDecompressPayload(
        JNIEnv *env, jobject obj,
        jbyteArray jdata, jint length)
{
    (void)obj;

    jbyte *raw = (*env)->GetByteArrayElements(env, jdata, NULL);
    if (!raw) return NULL;

    ByteReader r;
    reader_init(&r, (const uint8_t *)raw, (size_t)length);

    /* Validate magic */
    if (!reader_can_read(&r, 4)) goto fail;
    if (reader_u32le(&r) != MAGIC_RLE) goto fail;

    /* Original length */
    if (!reader_can_read(&r, 4)) goto fail;
    uint32_t orig_len = reader_u32le(&r);

    ByteBuffer out;
    if (!buf_init(&out, orig_len == 0 ? 1 : orig_len)) goto fail;

    while (r.pos < r.size) {
        if (!reader_can_read(&r, 3)) goto fail2;
        uint16_t count = reader_u16le(&r);
        uint8_t  val   = reader_u8(&r);
        if (!buf_ensure(&out, count)) goto fail2;
        memset(out.data + out.size, val, count);
        out.size += count;
    }

    (*env)->ReleaseByteArrayElements(env, jdata, raw, JNI_ABORT);

    {
        jbyteArray result = (*env)->NewByteArray(env, (jsize)out.size);
        if (result)
            (*env)->SetByteArrayRegion(env, result, 0, (jsize)out.size, (jbyte *)out.data);
        buf_free(&out);
        return result;
    }

fail2: buf_free(&out);
fail:
    (*env)->ReleaseByteArrayElements(env, jdata, raw, JNI_ABORT);
    return NULL;
}