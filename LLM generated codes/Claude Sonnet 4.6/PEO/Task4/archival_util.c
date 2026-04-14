#include <jni.h>
#include <stdint.h>     /* uint32_t, int64_t, uint8_t — exact-width types   */
#include <string.h>     /* memset                                            */
#include <stddef.h>     /* NULL                                              */
#include "DocumentMetadata.h"  /* generated via: javac -h . DocumentMetadata.java */

/* -------------------------------------------------------------------------
 * HEADER_SIZE: must match DocumentMetadata.HEADER_SIZE_BYTES exactly.
 *
 * Layout (all multi-byte fields in big-endian / network byte order):
 *   Offset  Size  Field
 *   0       4     ownerID            (uint32_t, big-endian)
 *   4       8     creationTimestamp  (int64_t,  big-endian)
 *   12      1     isEncrypted        (uint8_t,  0x00 or 0x01)
 *   13      3     reserved           (0x00 padding for future use)
 *   16      4     filePermissions    (uint32_t, big-endian)
 *   ─────────────────────────────────
 *   Total: 20 bytes — NO compiler-inserted padding anywhere
 * ------------------------------------------------------------------------- */
#define HEADER_SIZE 20

/* -------------------------------------------------------------------------
 * write_be32 / write_be64:
 * Write an unsigned 32-bit or 64-bit value in big-endian byte order into
 * a raw byte buffer at 'offset'. Using explicit byte shifts instead of
 * htonl()/htobe64() avoids host-endian assumptions and is fully portable.
 * ------------------------------------------------------------------------- */
static void write_be32(uint8_t *buf, size_t offset, uint32_t value) {
    buf[offset + 0] = (uint8_t)((value >> 24) & 0xFF);
    buf[offset + 1] = (uint8_t)((value >> 16) & 0xFF);
    buf[offset + 2] = (uint8_t)((value >>  8) & 0xFF);
    buf[offset + 3] = (uint8_t)( value        & 0xFF);
}

static void write_be64(uint8_t *buf, size_t offset, int64_t value) {
    /* Cast to uint64_t for well-defined bitwise behaviour on signed values */
    uint64_t uval = (uint64_t)value;
    buf[offset + 0] = (uint8_t)((uval >> 56) & 0xFF);
    buf[offset + 1] = (uint8_t)((uval >> 48) & 0xFF);
    buf[offset + 2] = (uint8_t)((uval >> 40) & 0xFF);
    buf[offset + 3] = (uint8_t)((uval >> 32) & 0xFF);
    buf[offset + 4] = (uint8_t)((uval >> 24) & 0xFF);
    buf[offset + 5] = (uint8_t)((uval >> 16) & 0xFF);
    buf[offset + 6] = (uint8_t)((uval >>  8) & 0xFF);
    buf[offset + 7] = (uint8_t)( uval        & 0xFF);
}

/* -------------------------------------------------------------------------
 * pack_header:
 * Core serialisation logic — pure C, zero JNI dependency.
 * Writes exactly HEADER_SIZE bytes into 'buf'.
 * Fully unit-testable without a JVM.
 * ------------------------------------------------------------------------- */
static void pack_header(uint8_t *buf,
                        jint     ownerID,
                        jlong    creationTimestamp,
                        jboolean isEncrypted,
                        jint     filePermissions) {

    /* Zero the entire buffer first — reserved/padding bytes become 0x00 */
    memset(buf, 0, HEADER_SIZE);

    /* Offset  0: ownerID (4 bytes, big-endian) */
    write_be32(buf, 0, (uint32_t)ownerID);

    /* Offset  4: creationTimestamp (8 bytes, big-endian) */
    write_be64(buf, 4, (int64_t)creationTimestamp);

    /* Offset 12: isEncrypted (1 byte) — normalised strictly to 0x00 or 0x01.
     * jboolean is uint8_t and JNI_TRUE == 1, but defensive normalisation
     * guards against any non-standard truthy value that could slip through. */
    buf[12] = (isEncrypted != JNI_FALSE) ? 0x01 : 0x00;

    /* Offset 13–15: reserved padding (already zeroed by memset above) */

    /* Offset 16: filePermissions (4 bytes, big-endian) */
    write_be32(buf, 16, (uint32_t)filePermissions);
}

/* -------------------------------------------------------------------------
 * APPROACH A:
 * Receives the full DocumentMetadata jobject.
 * All four fields are required here, so this is architecturally legitimate.
 * Still carries the maintenance risk of manual field-name string matching.
 * ------------------------------------------------------------------------- */
JNIEXPORT jbyteArray JNICALL
Java_DocumentMetadata_serializeToHeaderApproachA(JNIEnv *env, jobject thisObj) {

    /* --- Resolve the class --- */
    jclass cls = (*env)->GetObjectClass(env, thisObj);
    if (cls == NULL) { return NULL; }

    /* --- Resolve all four field IDs ---
     * JNI type descriptors:
     *   "I"  = int
     *   "J"  = long
     *   "Z"  = boolean
     * Field names are stringly-typed — a rename in Java without updating
     * these strings causes a NoSuchFieldError at runtime, not compile time. */
    jfieldID fid_ownerID   = (*env)->GetFieldID(env, cls, "ownerID",           "I");
    jfieldID fid_timestamp = (*env)->GetFieldID(env, cls, "creationTimestamp", "J");
    jfieldID fid_encrypted = (*env)->GetFieldID(env, cls, "isEncrypted",       "Z");
    jfieldID fid_perms     = (*env)->GetFieldID(env, cls, "filePermissions",   "I");

    if (fid_ownerID == NULL || fid_timestamp == NULL ||
        fid_encrypted == NULL || fid_perms == NULL) {
        return NULL;    /* Field resolution failed — fail safely */
    }

    /* --- Read all four fields --- */
    jint     ownerID           = (*env)->GetIntField    (env, thisObj, fid_ownerID);
    jlong    creationTimestamp = (*env)->GetLongField   (env, thisObj, fid_timestamp);
    jboolean isEncrypted       = (*env)->GetBooleanField(env, thisObj, fid_encrypted);
    jint     filePermissions   = (*env)->GetIntField    (env, thisObj, fid_perms);

    /* --- Allocate a Java byte array of exactly HEADER_SIZE bytes --- */
    jbyteArray result = (*env)->NewByteArray(env, HEADER_SIZE);
    if (result == NULL) { return NULL; }   /* OutOfMemoryError pending in JVM */

    /* --- Pack into a local C buffer then copy into the Java array --- */
    uint8_t buf[HEADER_SIZE];
    pack_header(buf, ownerID, creationTimestamp, isEncrypted, filePermissions);

    (*env)->SetByteArrayRegion(env, result, 0, HEADER_SIZE, (const jbyte *)buf);
    return result;
}

/* -------------------------------------------------------------------------
 * APPROACH B (RECOMMENDED):
 * Java's serializeToHeaderApproachB() extracted all four primitives before
 * crossing the JNI boundary. 'thisObj' is deliberately unused.
 *
 * Advantages over Approach A:
 *   • No GetFieldID / GetXxxField calls — no stringly-typed field names
 *   • pack_header() is testable in pure C without a JVM
 *   • If DocumentMetadata gains sensitive fields later, they cannot
 *     accidentally appear in this function's scope
 * ------------------------------------------------------------------------- */
JNIEXPORT jbyteArray JNICALL
Java_DocumentMetadata_nativeSerializeHeader(JNIEnv *env, jobject thisObj,
                                            jint     ownerID,
                                            jlong    creationTimestamp,
                                            jboolean isEncrypted,
                                            jint     filePermissions) {

    /* --- Allocate a Java byte array of exactly HEADER_SIZE bytes --- */
    jbyteArray result = (*env)->NewByteArray(env, HEADER_SIZE);
    if (result == NULL) { return NULL; }   /* OutOfMemoryError pending in JVM */

    /* --- Pack directly — no object introspection needed --- */
    uint8_t buf[HEADER_SIZE];
    pack_header(buf, ownerID, creationTimestamp, isEncrypted, filePermissions);

    (*env)->SetByteArrayRegion(env, result, 0, HEADER_SIZE, (const jbyte *)buf);
    return result;
}