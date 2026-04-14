/**
 * authentication_sweep.h
 *
 * Internal C++ types, constants, and function declarations used by
 * the JNI implementation of AuthenticationSweep.
 *
 * ── Minutiae IR wire format ──────────────────────────────────────
 *
 *  Offset  Size   Field
 *  ------  -----  ------------------------------------------------
 *   0       2 B   Magic bytes  { 0x4D 'M', 0x49 'I' }
 *   2       1 B   Version      IR_VERSION (0x01)
 *   3       1 B   Flags        reserved, must be 0x00
 *   4       2 B   Count        number of minutiae (little-endian)
 *   6       N×8B  Minutiae     each: [ x(2B) | y(2B) | angle(2B) | quality(2B) ]
 *
 * ── Signature IR wire format ────────────────────────────────────
 *
 *  Offset  Size    Field
 *  ------  ------  -----------------------------------------------
 *   0       2 B    Magic bytes  { 0x53 'S', 0x47 'G' }
 *   2       1 B    Version      IR_VERSION (0x01)
 *   3       1 B    Algorithm    SIG_ALGO_HMAC_SHA256 (0x01)
 *   4       2 B    Length       byte length of the signature body (little-endian)
 *   6       L B    Signature    raw cryptographic signature bytes
 */

#ifndef AUTHENTICATION_SWEEP_H
#define AUTHENTICATION_SWEEP_H

#include <jni.h>
#include <cstdint>
#include <cstddef>

/* ── Wire-format constants ──────────────────────────────────────── */
inline constexpr uint8_t  IR_VERSION           = 0x01;

// Minutiae IR magic
inline constexpr uint8_t  MIN_MAGIC_0          = 0x4Du; // 'M'
inline constexpr uint8_t  MIN_MAGIC_1          = 0x49u; // 'I'
inline constexpr size_t   MIN_HEADER_SIZE      = 6u;
inline constexpr size_t   MIN_ENTRY_SIZE       = 8u;    // x,y,angle,quality × 2B each
inline constexpr size_t   MAX_MINUTIAE_COUNT   = 200u;
inline constexpr uint8_t  MIN_QUALITY_THRESHOLD = 40u;  // 0-255 scale

// Signature IR magic
inline constexpr uint8_t  SIG_MAGIC_0          = 0x53u; // 'S'
inline constexpr uint8_t  SIG_MAGIC_1          = 0x47u; // 'G'
inline constexpr size_t   SIG_HEADER_SIZE      = 6u;
inline constexpr uint8_t  SIG_ALGO_HMAC_SHA256 = 0x01u;
inline constexpr size_t   SIG_BODY_SIZE        = 32u;   // HMAC-SHA256 = 256 bits

/* ── Biometric minutia record ────────────────────────────────────── */
struct Minutia {
    uint16_t x;        ///< column position in sensor grid
    uint16_t y;        ///< row position in sensor grid
    uint16_t angle;    ///< orientation angle (0–3599 in tenths of a degree)
    uint16_t quality;  ///< extraction confidence (0–255)
};

/* ── JNI helper declarations ─────────────────────────────────────── */

/**
 * Throws a Java AuthenticationException for the given stage.
 *
 * @param env         JNI environment
 * @param stage_field Java enum field name ("MINUTIAE_EXTRACTION" etc.)
 * @param message     non-sensitive error description
 * @return always nullptr — assign return value directly:
 *         return throwAuthEx(env, "MINUTIAE_EXTRACTION", "msg");
 */
void* throwAuthEx(JNIEnv* env,
                  const char* stage_field,
                  const char* message);

/**
 * Copies a jbyteArray into a heap-allocated buffer.
 * Caller must delete[] the result.
 *
 * @param env      JNI environment
 * @param arr      source jbyteArray
 * @param out_len  receives the number of bytes copied
 * @return heap buffer, or nullptr on allocation failure
 */
uint8_t* jbyteArrayToHeap(JNIEnv* env, jbyteArray arr, size_t& out_len);

/**
 * Creates a new jbyteArray from a heap buffer.
 *
 * @param env JNI environment
 * @param buf source bytes
 * @param len number of bytes
 * @return new jbyteArray, or nullptr if NewByteArray fails
 */
jbyteArray heapToJbyteArray(JNIEnv* env, const uint8_t* buf, size_t len);

/**
 * Securely zeroes then deallocates a heap buffer.
 * A no-op if buf is nullptr.
 */
void secureDelete(uint8_t* buf, size_t len);

#endif // AUTHENTICATION_SWEEP_H