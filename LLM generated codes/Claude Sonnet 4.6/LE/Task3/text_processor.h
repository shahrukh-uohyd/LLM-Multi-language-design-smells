/**
 * text_processor.h
 *
 * Internal C types and function declarations shared across the
 * text-processing pipeline implementation.
 *
 * This header is NOT exposed to the Java layer; it is an internal
 * contract between the C translation units only.
 *
 * IR wire format (text_ir_t layout stored in the byte[] passed via JNI):
 *
 *  Offset  Size   Field
 *  ------  ----   -----
 *   0       1B    IR_MAGIC    (0xTP = 0x54 'T' | 0x50 'P')  -- 2 bytes
 *   2       1B    version     (IR_VERSION = 0x01)
 *   3       1B    count       (number of elements, max 255)
 *   4       2B    total_len   (total byte length of all element strings, little-endian)
 *   6       …     elements    (count × [ 1B length | <length> bytes of UTF-8 text ])
 *
 * Processing state IR adds a 4-byte little-endian aggregated score
 * after the element block so nativeGenerate can read it directly.
 */

#ifndef TEXT_PROCESSOR_H
#define TEXT_PROCESSOR_H

#include <stddef.h>   /* size_t  */
#include <stdint.h>   /* uint8_t, uint16_t, uint32_t */

/* ── Magic bytes and version ──────────────────────────────────── */
#define IR_MAGIC_0   ((uint8_t)'T')
#define IR_MAGIC_1   ((uint8_t)'P')
#define IR_VERSION   ((uint8_t)0x01)

/* ── Per-element scoring weights (processing rules) ──────────── */
/** Base score contributed by each parsed element (token). */
#define SCORE_PER_ELEMENT   10u

/**
 * Bonus score added for every character in an element whose length
 * exceeds the SHORT_WORD threshold.
 */
#define LENGTH_BONUS_THRESHOLD  4u
#define SCORE_LENGTH_BONUS      5u

/* ── Limits ───────────────────────────────────────────────────── */
#define MAX_ELEMENTS    255u
#define MAX_ELEM_LEN    255u

/* ── Opaque IR buffer descriptor ─────────────────────────────── */
/**
 * A heap-allocated buffer together with its byte length.
 * The caller is responsible for freeing {@code data} with free().
 */
typedef struct {
    uint8_t* data;    /**< heap-allocated byte buffer                  */
    size_t   length;  /**< number of valid bytes in {@code data}        */
} ir_buffer_t;

/* ── Internal helper declarations ────────────────────────────── */

/**
 * Writes the 6-byte IR header into {@code buf}.
 * @param buf    destination (must have at least 6 bytes available)
 * @param count  number of elements to record in the header
 * @param total_len  combined byte length of all element payloads
 */
void ir_write_header(uint8_t* buf,
                     uint8_t  count,
                     uint16_t total_len);

/**
 * Validates that {@code buf[0..len-1]} starts with a well-formed header.
 * @return 1 if valid, 0 otherwise.
 */
int ir_validate_header(const uint8_t* buf, size_t len);

#endif /* TEXT_PROCESSOR_H */