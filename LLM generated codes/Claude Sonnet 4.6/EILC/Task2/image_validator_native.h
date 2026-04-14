#pragma once

// ── Error codes – must mirror ValidationResult Java constants ────────────────
#define IMG_ERR_NONE               0
#define IMG_ERR_UNSUPPORTED_FORMAT 1
#define IMG_ERR_INVALID_HEADER     2
#define IMG_ERR_DATA_TOO_SHORT     3
#define IMG_ERR_CORRUPTED_DATA     4

// ── Detected-format ordinals – must mirror ImageFormat Java enum ─────────────
#define IMG_FMT_UNKNOWN  0
#define IMG_FMT_JPEG     1
#define IMG_FMT_PNG      2
#define IMG_FMT_WEBP     3
#define IMG_FMT_GIF      4
#define IMG_FMT_BMP      5

// Minimum bytes required to read any magic-byte sequence
#define IMG_MAGIC_MIN_BYTES 12