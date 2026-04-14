/**
 * authentication_sweep.cpp
 *
 * JNI C++ implementation of the three biometric authentication stages
 * declared in AuthenticationSweep.java.
 *
 * ──────────────────────────────────────────────────────────────────
 *  Stage 1  Java_AuthenticationSweep_nativeExtractMinutiae
 *           Scans the raw sensor buffer, identifies ridge-ending and
 *           bifurcation minutiae above the quality threshold, and
 *           serialises them into the Minutiae IR.
 *
 *  Stage 2  Java_AuthenticationSweep_nativeGenerateSignature
 *           Deserialises the Minutiae IR, computes an HMAC-SHA-256
 *           digest over the canonical encoding of all minutiae, and
 *           wraps the result in the Signature IR.
 *
 *  Stage 3  Java_AuthenticationSweep_nativeTransmitToVault
 *           Validates the Signature IR, opens a secure channel to the
 *           hardware vault, transmits the signature, and returns the
 *           vault's acknowledgement token to Java.
 * ──────────────────────────────────────────────────────────────────
 *
 * Security notes
 * ──────────────
 * - All heap buffers holding biometric or cryptographic material are
 *   zeroed via secureDelete() before deallocation.
 * - No sensitive bytes are embedded in exception messages.
 * - The HMAC key is retrieved from the native security module on every
 *   call and zeroed immediately after use.
 */

#include "authentication_sweep.h"

#include <algorithm>
#include <cstring>
#include <sstream>
#include <stdexcept>
#include <string>

/* ═══════════════════════════════════════════════════════════════════
 *  Shared utility implementations
 * ═══════════════════════════════════════════════════════════════════ */

void* throwAuthEx(JNIEnv* env,
                  const char* stage_field,
                  const char* message)
{
    // Locate exception class
    jclass ex_cls = env->FindClass("AuthenticationException");
    if (!ex_cls) return nullptr; // FindClass already threw

    // Locate Stage enum class
    jclass stage_cls = env->FindClass("AuthenticationException$Stage");
    if (!stage_cls) return nullptr;

    // Retrieve the matching static enum constant
    jfieldID fid = env->GetStaticFieldID(
            stage_cls, stage_field, "LAuthenticationException$Stage;");
    if (!fid) return nullptr;

    jobject stage_obj = env->GetStaticObjectField(stage_cls, fid);
    if (!stage_obj) return nullptr;

    // Retrieve the (Stage, String) constructor
    jmethodID ctor = env->GetMethodID(
            ex_cls, "<init>",
            "(LAuthenticationException$Stage;Ljava/lang/String;)V");
    if (!ctor) return nullptr;

    jstring jmsg = env->NewStringUTF(message);
    jobject ex   = env->NewObject(ex_cls, ctor, stage_obj, jmsg);
    if (ex) env->Throw(static_cast<jthrowable>(ex));

    return nullptr;
}

uint8_t* jbyteArrayToHeap(JNIEnv* env, jbyteArray arr, size_t& out_len)
{
    if (!arr) { out_len = 0; return nullptr; }
    jsize  len = env->GetArrayLength(arr);
    jbyte* raw = env->GetByteArrayElements(arr, nullptr);
    if (!raw)  { out_len = 0; return nullptr; }

    auto* buf = new(std::nothrow) uint8_t[static_cast<size_t>(len)];
    if (buf) {
        std::memcpy(buf, raw, static_cast<size_t>(len));
        out_len = static_cast<size_t>(len);
    } else {
        out_len = 0;
    }
    env->ReleaseByteArrayElements(arr, raw, JNI_ABORT);
    return buf;
}

jbyteArray heapToJbyteArray(JNIEnv* env, const uint8_t* buf, size_t len)
{
    jbyteArray arr = env->NewByteArray(static_cast<jsize>(len));
    if (arr && len > 0)
        env->SetByteArrayRegion(arr, 0, static_cast<jsize>(len),
                                reinterpret_cast<const jbyte*>(buf));
    return arr;
}

void secureDelete(uint8_t* buf, size_t len)
{
    if (buf) {
        std::fill_n(buf, len, static_cast<uint8_t>(0x00));
        delete[] buf;
    }
}

/* ═══════════════════════════════════════════════════════════════════
 *  Internal helpers (not exported)
 * ═══════════════════════════════════════════════════════════════════ */
namespace {

/* ── Little-endian read / write helpers ─────────────────────────── */

inline uint16_t readU16LE(const uint8_t* p) {
    return static_cast<uint16_t>(p[0]) |
           (static_cast<uint16_t>(p[1]) << 8);
}

inline void writeU16LE(uint8_t* p, uint16_t v) {
    p[0] = static_cast<uint8_t>(v & 0xFFu);
    p[1] = static_cast<uint8_t>((v >> 8) & 0xFFu);
}

/* ── Biometric sensor simulation ────────────────────────────────── */
/**
 * Simulates minutiae extraction from a raw byte buffer.
 *
 * Production replacement: call the real biometric SDK function here.
 * Every non-zero byte whose index is a multiple of 8 is treated as
 * a candidate minutia; those with value >= MIN_QUALITY_THRESHOLD pass.
 */
bool extractMinutiaeFromBuffer(const uint8_t* buf, size_t len,
                                Minutia* out, size_t& count)
{
    count = 0;
    for (size_t i = 0; i + 8 <= len && count < MAX_MINUTIAE_COUNT; i += 8) {
        uint8_t quality = buf[i + 7];
        if (quality < MIN_QUALITY_THRESHOLD) continue;

        out[count].x       = static_cast<uint16_t>(buf[i]     | (buf[i+1] << 8));
        out[count].y       = static_cast<uint16_t>(buf[i+2]   | (buf[i+3] << 8));
        out[count].angle   = static_cast<uint16_t>(buf[i+4]   | (buf[i+5] << 8));
        out[count].quality = quality;
        ++count;
    }
    return count > 0;
}

/* ── Portable HMAC-SHA256 stub ───────────────────────────────────── */
/**
 * Computes a 32-byte digest over [data, data+len).
 *
 * Production replacement: call OpenSSL HMAC(), mbedTLS, or the
 * hardware-security-module API here.  This stub uses a deterministic
 * XOR-fold so the smoke-test is self-contained.
 */
void computeHmacSha256(const uint8_t* key,  size_t key_len,
                        const uint8_t* data, size_t data_len,
                        uint8_t out[SIG_BODY_SIZE])
{
    std::fill_n(out, SIG_BODY_SIZE, static_cast<uint8_t>(0x00));
    for (size_t i = 0; i < data_len; ++i)
        out[i % SIG_BODY_SIZE] ^= data[i];
    for (size_t i = 0; i < SIG_BODY_SIZE; ++i)
        out[i] ^= (key_len > 0 ? key[i % key_len] : 0x5Au);
}

/* ── Vault channel stub ─────────────────────────────────────────── */
/**
 * Transmits the signature to the hardware vault.
 *
 * Production replacement: open a TLS session / hardware bus channel
 * to the vault device, send the payload, and receive the token.
 *
 * Returns true and writes the token into out_token on success.
 */
bool transmitToVault(const uint8_t* sig_body, size_t sig_len,
                     const std::string& subject_id,
                     std::string& out_token)
{
    // Stub: always accept and fabricate an acknowledgement token
    // composed of the first 4 bytes of the signature in hex.
    (void)subject_id;
    char token[32];
    std::snprintf(token, sizeof(token),
                  "ACK-%02X%02X%02X%02X",
                  sig_len > 0 ? sig_body[0] : 0u,
                  sig_len > 1 ? sig_body[1] : 0u,
                  sig_len > 2 ? sig_body[2] : 0u,
                  sig_len > 3 ? sig_body[3] : 0u);
    out_token = token;
    return true;
}

} // anonymous namespace


/* ═══════════════════════════════════════════════════════════════════
 *  Stage 1 — nativeExtractMinutiae
 * ═══════════════════════════════════════════════════════════════════ */
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_AuthenticationSweep_nativeExtractMinutiae(JNIEnv*    env,
                                                jobject   /*self*/,
                                                jbyteArray jRawBuffer,
                                                jstring    jSubjectId)
{
    // ── 1a. Validate and copy inputs ─────────────────────────────── //
    if (!jRawBuffer)
        return static_cast<jbyteArray>(
            throwAuthEx(env, "MINUTIAE_EXTRACTION", "rawBuffer is null"));

    size_t   raw_len = 0;
    uint8_t* raw     = jbyteArrayToHeap(env, jRawBuffer, raw_len);
    if (!raw)
        return static_cast<jbyteArray>(
            throwAuthEx(env, "MINUTIAE_EXTRACTION",
                        "Failed to access raw buffer"));

    const char* subj_chars = env->GetStringUTFChars(jSubjectId, nullptr);
    std::string subject_id(subj_chars ? subj_chars : "");
    if (subj_chars) env->ReleaseStringUTFChars(jSubjectId, subj_chars);

    // ── 1b. Extract minutiae ─────────────────────────────────────── //
    Minutia minutiae[MAX_MINUTIAE_COUNT];
    size_t  count = 0;

    bool ok = extractMinutiaeFromBuffer(raw, raw_len, minutiae, count);
    secureDelete(raw, raw_len);  // zero and free raw sensor data immediately

    if (!ok || count == 0)
        return static_cast<jbyteArray>(
            throwAuthEx(env, "MINUTIAE_EXTRACTION",
                        "No minutiae above quality threshold detected"));

    // ── 1c. Serialise into Minutiae IR ────────────────────────────── //
    //  Header (6 B) + count × 8 B
    size_t   ir_len = MIN_HEADER_SIZE + count * MIN_ENTRY_SIZE;
    auto*    ir     = new(std::nothrow) uint8_t[ir_len];
    if (!ir)
        return static_cast<jbyteArray>(
            throwAuthEx(env, "MINUTIAE_EXTRACTION",
                        "Out of memory allocating Minutiae IR"));

    ir[0] = MIN_MAGIC_0;
    ir[1] = MIN_MAGIC_1;
    ir[2] = IR_VERSION;
    ir[3] = 0x00;  // flags reserved
    writeU16LE(ir + 4, static_cast<uint16_t>(count));

    uint8_t* cur = ir + MIN_HEADER_SIZE;
    for (size_t i = 0; i < count; ++i) {
        writeU16LE(cur + 0, minutiae[i].x);
        writeU16LE(cur + 2, minutiae[i].y);
        writeU16LE(cur + 4, minutiae[i].angle);
        writeU16LE(cur + 6, minutiae[i].quality);
        cur += MIN_ENTRY_SIZE;
    }

    jbyteArray result = heapToJbyteArray(env, ir, ir_len);
    secureDelete(ir, ir_len);
    return result;
}


/* ═══════════════════════════════════════════════════════════════════
 *  Stage 2 — nativeGenerateSignature
 * ═══════════════════════════════════════════════════════════════════ */
extern "C"
JNIEXPORT jbyteArray JNICALL
Java_AuthenticationSweep_nativeGenerateSignature(JNIEnv*    env,
                                                  jobject   /*self*/,
                                                  jbyteArray jMinutiaeFeatures,
                                                  jstring    jSubjectId)
{
    // ── 2a. Copy Minutiae IR ─────────────────────────────────────── //
    size_t   ir_len = 0;
    uint8_t* ir     = jbyteArrayToHeap(env, jMinutiaeFeatures, ir_len);
    if (!ir)
        return static_cast<jbyteArray>(
            throwAuthEx(env, "SIGNATURE_GENERATION",
                        "Failed to access Minutiae IR"));

    // ── 2b. Validate Minutiae IR header ──────────────────────────── //
    if (ir_len < MIN_HEADER_SIZE
            || ir[0] != MIN_MAGIC_0
            || ir[1] != MIN_MAGIC_1
            || ir[2] != IR_VERSION) {
        secureDelete(ir, ir_len);
        return static_cast<jbyteArray>(
            throwAuthEx(env, "SIGNATURE_GENERATION",
                        "Minutiae IR has invalid or corrupt header"));
    }

    uint16_t count = readU16LE(ir + 4);
    size_t   expected_len = MIN_HEADER_SIZE + static_cast<size_t>(count) * MIN_ENTRY_SIZE;
    if (ir_len != expected_len) {
        secureDelete(ir, ir_len);
        return static_cast<jbyteArray>(
            throwAuthEx(env, "SIGNATURE_GENERATION",
                        "Minutiae IR length does not match declared count"));
    }

    // ── 2c. Retrieve HMAC key from native security module ────────── //
    //  Production: replace with HSM key-retrieval or keystore API call.
    constexpr size_t KEY_LEN = 32u;
    uint8_t hmac_key[KEY_LEN];
    // Stub: derive a fixed key from subject ID length for portability
    for (size_t i = 0; i < KEY_LEN; ++i)
        hmac_key[i] = static_cast<uint8_t>(0xA5u ^ (i & 0xFFu));

    // ── 2d. Compute HMAC-SHA-256 over the element block (not header) //
    uint8_t digest[SIG_BODY_SIZE];
    computeHmacSha256(hmac_key,   KEY_LEN,
                       ir + MIN_HEADER_SIZE,
                       ir_len - MIN_HEADER_SIZE,
                       digest);

    // Zero key and IR immediately
    std::fill_n(hmac_key, KEY_LEN, static_cast<uint8_t>(0x00));
    secureDelete(ir, ir_len);

    // ── 2e. Build Signature IR ───────────────────────────────────── //
    //  Header (6 B) + signature body (32 B)
    size_t   sig_ir_len = SIG_HEADER_SIZE + SIG_BODY_SIZE;
    auto*    sig_ir     = new(std::nothrow) uint8_t[sig_ir_len];
    if (!sig_ir)
        return static_cast<jbyteArray>(
            throwAuthEx(env, "SIGNATURE_GENERATION",
                        "Out of memory allocating Signature IR"));

    sig_ir[0] = SIG_MAGIC_0;
    sig_ir[1] = SIG_MAGIC_1;
    sig_ir[2] = IR_VERSION;
    sig_ir[3] = SIG_ALGO_HMAC_SHA256;
    writeU16LE(sig_ir + 4, static_cast<uint16_t>(SIG_BODY_SIZE));
    std::memcpy(sig_ir + SIG_HEADER_SIZE, digest, SIG_BODY_SIZE);
    std::fill_n(digest, SIG_BODY_SIZE, static_cast<uint8_t>(0x00)); // zero digest

    jbyteArray result = heapToJbyteArray(env, sig_ir, sig_ir_len);
    secureDelete(sig_ir, sig_ir_len);
    return result;
}


/* ═══════════════════════════════════════════════════════════════════
 *  Stage 3 — nativeTransmitToVault
 * ═══════════════════════════════════════════════════════════════════ */
extern "C"
JNIEXPORT jstring JNICALL
Java_AuthenticationSweep_nativeTransmitToVault(JNIEnv*    env,
                                                jobject   /*self*/,
                                                jbyteArray jSignature,
                                                jstring    jSubjectId)
{
    // ── 3a. Copy Signature IR ────────────────────────────────────── //
    size_t   sig_ir_len = 0;
    uint8_t* sig_ir     = jbyteArrayToHeap(env, jSignature, sig_ir_len);
    if (!sig_ir)
        return static_cast<jstring>(
            throwAuthEx(env, "VAULT_TRANSMISSION",
                        "Failed to access Signature IR"));

    // ── 3b. Validate Signature IR header ─────────────────────────── //
    if (sig_ir_len < SIG_HEADER_SIZE
            || sig_ir[0] != SIG_MAGIC_0
            || sig_ir[1] != SIG_MAGIC_1
            || sig_ir[2] != IR_VERSION) {
        secureDelete(sig_ir, sig_ir_len);
        return static_cast<jstring>(
            throwAuthEx(env, "VAULT_TRANSMISSION",
                        "Signature IR has invalid or corrupt header"));
    }

    uint16_t sig_body_len = readU16LE(sig_ir + 4);
    if (sig_ir_len != SIG_HEADER_SIZE + static_cast<size_t>(sig_body_len)) {
        secureDelete(sig_ir, sig_ir_len);
        return static_cast<jstring>(
            throwAuthEx(env, "VAULT_TRANSMISSION",
                        "Signature IR length does not match declared body length"));
    }

    // ── 3c. Extract subject ID ───────────────────────────────────── //
    const char* subj_chars = env->GetStringUTFChars(jSubjectId, nullptr);
    std::string subject_id(subj_chars ? subj_chars : "");
    if (subj_chars) env->ReleaseStringUTFChars(jSubjectId, subj_chars);

    // ── 3d. Transmit to vault and receive acknowledgement ─────────── //
    const uint8_t* sig_body = sig_ir + SIG_HEADER_SIZE;
    std::string ack_token;

    bool accepted = transmitToVault(sig_body, sig_body_len,
                                     subject_id, ack_token);
    secureDelete(sig_ir, sig_ir_len); // zero signature before dealloc

    if (!accepted || ack_token.empty()) {
        return static_cast<jstring>(
            throwAuthEx(env, "VAULT_TRANSMISSION",
                        "Vault rejected the signature or returned no acknowledgement"));
    }

    return env->NewStringUTF(ack_token.c_str());
}