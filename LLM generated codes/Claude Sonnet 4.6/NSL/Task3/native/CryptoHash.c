/*
 * JNI implementation of cryptographic hash functions.
 *
 * Algorithms implemented (pure ANSI C, no third-party deps):
 *   MD5     – RFC 1321
 *   SHA-1   – FIPS PUB 180-4
 *   SHA-256 – FIPS PUB 180-4
 *   SHA-512 – FIPS PUB 180-4
 *
 * Each algorithm follows the same pattern:
 *   1.  _init()   – initialise context / state words
 *   2.  _update() – process an arbitrary-length message in 512/1024-bit blocks
 *   3.  _final()  – append padding, process last block(s), serialise digest
 */

#include "CryptoHash.h"
#include <string.h>
#include <stdlib.h>
#include <stdint.h>

/* ======================================================================
 * Portable byte-order helpers
 * ====================================================================== */

static uint32_t u32_big(uint32_t x) {
    return ((x & 0xFFu)       << 24) |
           ((x & 0xFF00u)     <<  8) |
           ((x & 0xFF0000u)   >>  8) |
           ((x & 0xFF000000u) >> 24);
}

static uint64_t u64_big(uint64_t x) {
    return ((uint64_t)u32_big((uint32_t)(x & 0xFFFFFFFFull)) << 32) |
                      u32_big((uint32_t)(x >> 32));
}

/* ======================================================================
 *  ██████   MD5   (RFC 1321)
 * ====================================================================== */

#define MD5_DIGEST_LEN 16
#define MD5_BLOCK_LEN  64

typedef struct {
    uint32_t state[4];
    uint64_t count;
    uint8_t  buf[MD5_BLOCK_LEN];
} MD5_CTX;

/* per-round shift amounts */
static const uint32_t MD5_S[64] = {
    7,12,17,22, 7,12,17,22, 7,12,17,22, 7,12,17,22,
    5, 9,14,20, 5, 9,14,20, 5, 9,14,20, 5, 9,14,20,
    4,11,16,23, 4,11,16,23, 4,11,16,23, 4,11,16,23,
    6,10,15,21, 6,10,15,21, 6,10,15,21, 6,10,15,21
};

/* precomputed T[i] = floor(2^32 * abs(sin(i+1))) */
static const uint32_t MD5_T[64] = {
    0xd76aa478,0xe8c7b756,0x242070db,0xc1bdceee,
    0xf57c0faf,0x4787c62a,0xa8304613,0xfd469501,
    0x698098d8,0x8b44f7af,0xffff5bb1,0x895cd7be,
    0x6b901122,0xfd987193,0xa679438e,0x49b40821,
    0xf61e2562,0xc040b340,0x265e5a51,0xe9b6c7aa,
    0xd62f105d,0x02441453,0xd8a1e681,0xe7d3fbc8,
    0x21e1cde6,0xc33707d6,0xf4d50d87,0x455a14ed,
    0xa9e3e905,0xfcefa3f8,0x676f02d9,0x8d2a4c8a,
    0xfffa3942,0x8771f681,0x6d9d6122,0xfde5380c,
    0xa4beea44,0x4bdecfa9,0xf6bb4b60,0xbebfbc70,
    0x289b7ec6,0xeaa127fa,0xd4ef3085,0x04881d05,
    0xd9d4d039,0xe6db99e5,0x1fa27cf8,0xc4ac5665,
    0xf4292244,0x432aff97,0xab9423a7,0xfc93a039,
    0x655b59c3,0x8f0ccc92,0xffeff47d,0x85845dd1,
    0x6fa87e4f,0xfe2ce6e0,0xa3014314,0x4e0811a1,
    0xf7537e82,0xbd3af235,0x2ad7d2bb,0xeb86d391
};

#define MD5_RL(x,n) (((x)<<(n))|((x)>>(32-(n))))
#define MD5_F(b,c,d) (((b)&(c))|((~(b))&(d)))
#define MD5_G(b,c,d) (((b)&(d))|((c)&(~(d))))
#define MD5_H(b,c,d) ((b)^(c)^(d))
#define MD5_I(b,c,d) ((c)^((b)|(~(d))))

static void md5_transform(uint32_t state[4], const uint8_t block[MD5_BLOCK_LEN]) {
    uint32_t a = state[0], b = state[1], c = state[2], d = state[3];
    uint32_t M[16];
    int i;
    for (i = 0; i < 16; i++)
        M[i] = (uint32_t)block[i*4]        | ((uint32_t)block[i*4+1] <<  8) |
               ((uint32_t)block[i*4+2] << 16) | ((uint32_t)block[i*4+3] << 24);

    for (i = 0; i < 64; i++) {
        uint32_t f, g;
        if      (i < 16) { f = MD5_F(b,c,d); g = (uint32_t)i; }
        else if (i < 32) { f = MD5_G(b,c,d); g = (5*(uint32_t)i+1)%16; }
        else if (i < 48) { f = MD5_H(b,c,d); g = (3*(uint32_t)i+5)%16; }
        else             { f = MD5_I(b,c,d); g = (7*(uint32_t)i)%16; }
        f = f + a + MD5_T[i] + M[g];
        a = d; d = c; c = b;
        b = b + MD5_RL(f, MD5_S[i]);
    }
    state[0] += a; state[1] += b; state[2] += c; state[3] += d;
}

static void md5_init(MD5_CTX *ctx) {
    ctx->state[0] = 0x67452301u;
    ctx->state[1] = 0xefcdab89u;
    ctx->state[2] = 0x98badcfeu;
    ctx->state[3] = 0x10325476u;
    ctx->count = 0;
}

static void md5_update(MD5_CTX *ctx, const uint8_t *data, size_t len) {
    size_t i;
    uint32_t idx = (uint32_t)((ctx->count >> 3) & 0x3F);
    ctx->count += (uint64_t)len << 3;
    uint32_t part = MD5_BLOCK_LEN - idx;
    if (len >= part) {
        memcpy(ctx->buf + idx, data, part);
        md5_transform(ctx->state, ctx->buf);
        for (i = part; i + MD5_BLOCK_LEN - 1 < len; i += MD5_BLOCK_LEN)
            md5_transform(ctx->state, data + i);
        idx = 0;
    } else { i = 0; }
    memcpy(ctx->buf + idx, data + i, len - i);
}

static void md5_final(MD5_CTX *ctx, uint8_t digest[MD5_DIGEST_LEN]) {
    static const uint8_t PAD[MD5_BLOCK_LEN] = { 0x80 };
    uint8_t bits[8];
    uint64_t cnt = ctx->count;
    int i;
    for (i = 0; i < 8; i++) bits[i] = (uint8_t)(cnt >> (i*8));
    uint32_t idx = (uint32_t)((ctx->count >> 3) & 0x3F);
    uint32_t pad = (idx < 56) ? (56 - idx) : (120 - idx);
    md5_update(ctx, PAD, pad);
    md5_update(ctx, bits, 8);
    for (i = 0; i < 4; i++) {
        digest[i*4]   = (uint8_t)(ctx->state[i]);
        digest[i*4+1] = (uint8_t)(ctx->state[i] >>  8);
        digest[i*4+2] = (uint8_t)(ctx->state[i] >> 16);
        digest[i*4+3] = (uint8_t)(ctx->state[i] >> 24);
    }
}

/* ======================================================================
 *  ███████  SHA-1  (FIPS 180-4)
 * ====================================================================== */

#define SHA1_DIGEST_LEN 20
#define SHA1_BLOCK_LEN  64

typedef struct {
    uint32_t state[5];
    uint64_t count;
    uint8_t  buf[SHA1_BLOCK_LEN];
    uint32_t bufLen;
} SHA1_CTX;

#define SHA1_RL(x,n) (((x)<<(n))|((x)>>(32-(n))))
#define SHA1_CH(x,y,z)  (((x)&(y))^((~(x))&(z)))
#define SHA1_PAR(x,y,z) ((x)^(y)^(z))
#define SHA1_MAJ(x,y,z) (((x)&(y))^((x)&(z))^((y)&(z)))

static void sha1_transform(uint32_t state[5], const uint8_t block[SHA1_BLOCK_LEN]) {
    uint32_t W[80];
    int t;
    for (t = 0;  t < 16; t++)
        W[t] = ((uint32_t)block[t*4]<<24)|((uint32_t)block[t*4+1]<<16)|
               ((uint32_t)block[t*4+2]<<8)|(uint32_t)block[t*4+3];
    for (t = 16; t < 80; t++)
        W[t] = SHA1_RL(W[t-3]^W[t-8]^W[t-14]^W[t-16], 1);

    uint32_t a=state[0],b=state[1],c=state[2],d=state[3],e=state[4];
    for (t = 0; t < 80; t++) {
        uint32_t f, k;
        if      (t < 20) { f = SHA1_CH (b,c,d); k = 0x5a827999u; }
        else if (t < 40) { f = SHA1_PAR(b,c,d); k = 0x6ed9eba1u; }
        else if (t < 60) { f = SHA1_MAJ(b,c,d); k = 0x8f1bbcdcu; }
        else             { f = SHA1_PAR(b,c,d); k = 0xca62c1d6u; }
        uint32_t tmp = SHA1_RL(a,5) + f + e + k + W[t];
        e=d; d=c; c=SHA1_RL(b,30); b=a; a=tmp;
    }
    state[0]+=a; state[1]+=b; state[2]+=c; state[3]+=d; state[4]+=e;
}

static void sha1_init(SHA1_CTX *ctx) {
    ctx->state[0]=0x67452301u; ctx->state[1]=0xefcdab89u;
    ctx->state[2]=0x98badcfeu; ctx->state[3]=0x10325476u;
    ctx->state[4]=0xc3d2e1f0u;
    ctx->count=0; ctx->bufLen=0;
}

static void sha1_update(SHA1_CTX *ctx, const uint8_t *data, size_t len) {
    size_t i = 0;
    ctx->count += (uint64_t)len * 8;
    while (i < len) {
        ctx->buf[ctx->bufLen++] = data[i++];
        if (ctx->bufLen == SHA1_BLOCK_LEN) {
            sha1_transform(ctx->state, ctx->buf);
            ctx->bufLen = 0;
        }
    }
}

static void sha1_final(SHA1_CTX *ctx, uint8_t digest[SHA1_DIGEST_LEN]) {
    ctx->buf[ctx->bufLen++] = 0x80;
    if (ctx->bufLen > 56) {
        while (ctx->bufLen < SHA1_BLOCK_LEN) ctx->buf[ctx->bufLen++] = 0;
        sha1_transform(ctx->state, ctx->buf);
        ctx->bufLen = 0;
    }
    while (ctx->bufLen < 56) ctx->buf[ctx->bufLen++] = 0;
    uint64_t bcount = ctx->count;
    int i;
    for (i = 7; i >= 0; i--) { ctx->buf[56+i] = (uint8_t)(bcount & 0xFF); bcount >>= 8; }
    sha1_transform(ctx->state, ctx->buf);
    for (i = 0; i < 5; i++) {
        digest[i*4]   = (uint8_t)(ctx->state[i]>>24);
        digest[i*4+1] = (uint8_t)(ctx->state[i]>>16);
        digest[i*4+2] = (uint8_t)(ctx->state[i]>> 8);
        digest[i*4+3] = (uint8_t)(ctx->state[i]    );
    }
}

/* ======================================================================
 *  ████████  SHA-256  (FIPS 180-4)
 * ====================================================================== */

#define SHA256_DIGEST_LEN 32
#define SHA256_BLOCK_LEN  64

static const uint32_t SHA256_K[64] = {
    0x428a2f98u,0x71374491u,0xb5c0fbcfu,0xe9b5dba5u,
    0x3956c25bu,0x59f111f1u,0x923f82a4u,0xab1c5ed5u,
    0xd807aa98u,0x12835b01u,0x243185beu,0x550c7dc3u,
    0x72be5d74u,0x80deb1feu,0x9bdc06a7u,0xc19bf174u,
    0xe49b69c1u,0xefbe4786u,0x0fc19dc6u,0x240ca1ccu,
    0x2de92c6fu,0x4a7484aau,0x5cb0a9dcu,0x76f988dau,
    0x983e5152u,0xa831c66du,0xb00327c8u,0xbf597fc7u,
    0xc6e00bf3u,0xd5a79147u,0x06ca6351u,0x14292967u,
    0x27b70a85u,0x2e1b2138u,0x4d2c6dfcu,0x53380d13u,
    0x650a7354u,0x766a0abbu,0x81c2c92eu,0x92722c85u,
    0xa2bfe8a1u,0xa81a664bu,0xc24b8b70u,0xc76c51a3u,
    0xd192e819u,0xd6990624u,0xf40e3585u,0x106aa070u,
    0x19a4c116u,0x1e376c08u,0x2748774cu,0x34b0bcb5u,
    0x391c0cb3u,0x4ed8aa4au,0x5b9cca4fu,0x682e6ff3u,
    0x748f82eeu,0x78a5636fu,0x84c87814u,0x8cc70208u,
    0x90befffau,0xa4506cebu,0xbef9a3f7u,0xc67178f2u
};

typedef struct {
    uint32_t state[8];
    uint64_t count;
    uint8_t  buf[SHA256_BLOCK_LEN];
    uint32_t bufLen;
} SHA256_CTX;

#define SHA256_RR(x,n) (((x)>>(n))|((x)<<(32-(n))))
#define SHA256_S0(x)   (SHA256_RR(x,2)^SHA256_RR(x,13)^SHA256_RR(x,22))
#define SHA256_S1(x)   (SHA256_RR(x,6)^SHA256_RR(x,11)^SHA256_RR(x,25))
#define SHA256_s0(x)   (SHA256_RR(x,7)^SHA256_RR(x,18)^((x)>>3))
#define SHA256_s1(x)   (SHA256_RR(x,17)^SHA256_RR(x,19)^((x)>>10))
#define SHA256_CH(x,y,z)  (((x)&(y))^((~(x))&(z)))
#define SHA256_MAJ(x,y,z) (((x)&(y))^((x)&(z))^((y)&(z)))

static void sha256_transform(uint32_t state[8],
                              const uint8_t block[SHA256_BLOCK_LEN]) {
    uint32_t W[64];
    int t;
    for (t = 0;  t < 16; t++)
        W[t] = ((uint32_t)block[t*4]<<24)|((uint32_t)block[t*4+1]<<16)|
               ((uint32_t)block[t*4+2]<<8)|(uint32_t)block[t*4+3];
    for (t = 16; t < 64; t++)
        W[t] = SHA256_s1(W[t-2]) + W[t-7] + SHA256_s0(W[t-15]) + W[t-16];

    uint32_t a=state[0],b=state[1],c=state[2],d=state[3];
    uint32_t e=state[4],f=state[5],g=state[6],h=state[7];
    for (t = 0; t < 64; t++) {
        uint32_t t1 = h + SHA256_S1(e) + SHA256_CH(e,f,g) + SHA256_K[t] + W[t];
        uint32_t t2 = SHA256_S0(a) + SHA256_MAJ(a,b,c);
        h=g; g=f; f=e; e=d+t1; d=c; c=b; b=a; a=t1+t2;
    }
    state[0]+=a; state[1]+=b; state[2]+=c; state[3]+=d;
    state[4]+=e; state[5]+=f; state[6]+=g; state[7]+=h;
}

static void sha256_init(SHA256_CTX *ctx) {
    ctx->state[0]=0x6a09e667u; ctx->state[1]=0xbb67ae85u;
    ctx->state[2]=0x3c6ef372u; ctx->state[3]=0xa54ff53au;
    ctx->state[4]=0x510e527fu; ctx->state[5]=0x9b05688cu;
    ctx->state[6]=0x1f83d9abu; ctx->state[7]=0x5be0cd19u;
    ctx->count=0; ctx->bufLen=0;
}

static void sha256_update(SHA256_CTX *ctx, const uint8_t *data, size_t len) {
    size_t i = 0;
    ctx->count += (uint64_t)len * 8;
    while (i < len) {
        ctx->buf[ctx->bufLen++] = data[i++];
        if (ctx->bufLen == SHA256_BLOCK_LEN) {
            sha256_transform(ctx->state, ctx->buf);
            ctx->bufLen = 0;
        }
    }
}

static void sha256_final(SHA256_CTX *ctx, uint8_t digest[SHA256_DIGEST_LEN]) {
    ctx->buf[ctx->bufLen++] = 0x80;
    if (ctx->bufLen > 56) {
        while (ctx->bufLen < SHA256_BLOCK_LEN) ctx->buf[ctx->bufLen++] = 0;
        sha256_transform(ctx->state, ctx->buf);
        ctx->bufLen = 0;
    }
    while (ctx->bufLen < 56) ctx->buf[ctx->bufLen++] = 0;
    uint64_t bc = ctx->count;
    int i;
    for (i = 7; i >= 0; i--) { ctx->buf[56+i]=(uint8_t)(bc&0xFF); bc>>=8; }
    sha256_transform(ctx->state, ctx->buf);
    for (i = 0; i < 8; i++) {
        digest[i*4]   = (uint8_t)(ctx->state[i]>>24);
        digest[i*4+1] = (uint8_t)(ctx->state[i]>>16);
        digest[i*4+2] = (uint8_t)(ctx->state[i]>> 8);
        digest[i*4+3] = (uint8_t)(ctx->state[i]    );
    }
}

/* ======================================================================
 *  ██████████  SHA-512  (FIPS 180-4)
 * ====================================================================== */

#define SHA512_DIGEST_LEN 64
#define SHA512_BLOCK_LEN  128

static const uint64_t SHA512_K[80] = {
    0x428a2f98d728ae22ull,0x7137449123ef65cdull,0xb5c0fbcfec4d3b2full,
    0xe9b5dba58189dbbcull,0x3956c25bf348b538ull,0x59f111f1b605d019ull,
    0x923f82a4af194f9bull,0xab1c5ed5da6d8118ull,0xd807aa98a3030242ull,
    0x12835b0145706fbeull,0x243185be4ee4b28cull,0x550c7dc3d5ffb4e2ull,
    0x72be5d74f27b896full,0x80deb1fe3b1696b1ull,0x9bdc06a725c71235ull,
    0xc19bf174cf692694ull,0xe49b69c19ef14ad2ull,0xefbe4786384f25e3ull,
    0x0fc19dc68b8cd5b5ull,0x240ca1cc77ac9c65ull,0x2de92c6f592b0275ull,
    0x4a7484aa6ea6e483ull,0x5cb0a9dcbd41fbd4ull,0x76f988da831153b5ull,
    0x983e5152ee66dfabull,0xa831c66d2db43210ull,0xb00327c898fb213full,
    0xbf597fc7beef0ee4ull,0xc6e00bf33da88fc2ull,0xd5a79147930aa725ull,
    0x06ca6351e003826full,0x142929670a0e6e70ull,0x27b70a8546d22ffcull,
    0x2e1b21385c26c926ull,0x4d2c6dfc5ac42aedull,0x53380d139d95b3dfull,
    0x650a73548baf63deull,0x766a0abb3c77b2a8ull,0x81c2c92e47edaee6ull,
    0x92722c851482353bull,0xa2bfe8a14cf10364ull,0xa81a664bbc423001ull,
    0xc24b8b70d0f89791ull,0xc76c51a30654be30ull,0xd192e819d6ef5218ull,
    0xd69906245565a910ull,0xf40e35855771202aull,0x106aa07032bbd1b8ull,
    0x19a4c116b8d2d0c8ull,0x1e376c085141ab53ull,0x2748774cdf8eeb99ull,
    0x34b0bcb5e19b48a8ull,0x391c0cb3c5c95a63ull,0x4ed8aa4ae3418acbull,
    0x5b9cca4f7763e373ull,0x682e6ff3d6b2b8a3ull,0x748f82ee5defb2fcull,
    0x78a5636f43172f60ull,0x84c87814a1f0ab72ull,0x8cc702081a6439ecull,
    0x90befffa23631e28ull,0xa4506cebde82bde9ull,0xbef9a3f7b2c67915ull,
    0xc67178f2e372532bull,0xca273eceea26619cull,0xd186b8c721c0c207ull,
    0xeada7dd6cde0eb1eull,0xf57d4f7fee6ed178ull,0x06f067aa72176fbaull,
    0x0a637dc5a2c898a6ull,0x113f9804bef90daeull,0x1b710b35131c471bull,
    0x28db77f523047d84ull,0x32caab7b40c72493ull,0x3c9ebe0a15c9bebcull,
    0x431d67c49c100d4cull,0x4cc5d4becb3e42b6ull,0x597f299cfc657e2aull,
    0x5fcb6fab3ad6faecull,0x6c44198c4a475817ull
};

typedef struct {
    uint64_t state[8];
    uint64_t countHi, countLo;
    uint8_t  buf[SHA512_BLOCK_LEN];
    uint32_t bufLen;
} SHA512_CTX;

#define SHA512_RR(x,n) (((x)>>(n))|((x)<<(64-(n))))
#define SHA512_S0(x)   (SHA512_RR(x,28)^SHA512_RR(x,34)^SHA512_RR(x,39))
#define SHA512_S1(x)   (SHA512_RR(x,14)^SHA512_RR(x,18)^SHA512_RR(x,41))
#define SHA512_s0(x)   (SHA512_RR(x, 1)^SHA512_RR(x, 8)^((x)>>7))
#define SHA512_s1(x)   (SHA512_RR(x,19)^SHA512_RR(x,61)^((x)>>6))
#define SHA512_CH(x,y,z)  (((x)&(y))^((~(x))&(z)))
#define SHA512_MAJ(x,y,z) (((x)&(y))^((x)&(z))^((y)&(z)))

static void sha512_transform(uint64_t state[8],
                              const uint8_t block[SHA512_BLOCK_LEN]) {
    uint64_t W[80];
    int t;
    for (t = 0;  t < 16; t++)
        W[t] = ((uint64_t)block[t*8  ]<<56)|((uint64_t)block[t*8+1]<<48)|
               ((uint64_t)block[t*8+2]<<40)|((uint64_t)block[t*8+3]<<32)|
               ((uint64_t)block[t*8+4]<<24)|((uint64_t)block[t*8+5]<<16)|
               ((uint64_t)block[t*8+6]<< 8)|(uint64_t)block[t*8+7];
    for (t = 16; t < 80; t++)
        W[t] = SHA512_s1(W[t-2])+W[t-7]+SHA512_s0(W[t-15])+W[t-16];

    uint64_t a=state[0],b=state[1],c=state[2],d=state[3];
    uint64_t e=state[4],f=state[5],g=state[6],h=state[7];
    for (t = 0; t < 80; t++) {
        uint64_t t1 = h + SHA512_S1(e) + SHA512_CH(e,f,g) + SHA512_K[t] + W[t];
        uint64_t t2 = SHA512_S0(a) + SHA512_MAJ(a,b,c);
        h=g; g=f; f=e; e=d+t1; d=c; c=b; b=a; a=t1+t2;
    }
    state[0]+=a; state[1]+=b; state[2]+=c; state[3]+=d;
    state[4]+=e; state[5]+=f; state[6]+=g; state[7]+=h;
}

static void sha512_init(SHA512_CTX *ctx) {
    ctx->state[0]=0x6a09e667f3bcc908ull; ctx->state[1]=0xbb67ae8584caa73bull;
    ctx->state[2]=0x3c6ef372fe94f82bull; ctx->state[3]=0xa54ff53a5f1d36f1ull;
    ctx->state[4]=0x510e527fade682d1ull; ctx->state[5]=0x9b05688c2b3e6c1full;
    ctx->state[6]=0x1f83d9abfb41bd6bull; ctx->state[7]=0x5be0cd19137e2179ull;
    ctx->countHi=0; ctx->countLo=0; ctx->bufLen=0;
}

static void sha512_update(SHA512_CTX *ctx, const uint8_t *data, size_t len) {
    size_t i = 0;
    uint64_t lo = ctx->countLo + ((uint64_t)len << 3);
    if (lo < ctx->countLo) ctx->countHi++;
    ctx->countLo = lo;
    while (i < len) {
        ctx->buf[ctx->bufLen++] = data[i++];
        if (ctx->bufLen == SHA512_BLOCK_LEN) {
            sha512_transform(ctx->state, ctx->buf);
            ctx->bufLen = 0;
        }
    }
}

static void sha512_final(SHA512_CTX *ctx, uint8_t digest[SHA512_DIGEST_LEN]) {
    ctx->buf[ctx->bufLen++] = 0x80;
    if (ctx->bufLen > 112) {
        while (ctx->bufLen < SHA512_BLOCK_LEN) ctx->buf[ctx->bufLen++] = 0;
        sha512_transform(ctx->state, ctx->buf);
        ctx->bufLen = 0;
    }
    while (ctx->bufLen < 112) ctx->buf[ctx->bufLen++] = 0;
    /* append 128-bit big-endian bit count */
    uint64_t hi = ctx->countHi, lo = ctx->countLo;
    int i;
    for (i = 7; i >= 0; i--) { ctx->buf[120+i]=(uint8_t)(lo&0xFF); lo>>=8; }
    for (i = 7; i >= 0; i--) { ctx->buf[112+i]=(uint8_t)(hi&0xFF); hi>>=8; }
    sha512_transform(ctx->state, ctx->buf);
    for (i = 0; i < 8; i++) {
        digest[i*8]   = (uint8_t)(ctx->state[i]>>56);
        digest[i*8+1] = (uint8_t)(ctx->state[i]>>48);
        digest[i*8+2] = (uint8_t)(ctx->state[i]>>40);
        digest[i*8+3] = (uint8_t)(ctx->state[i]>>32);
        digest[i*8+4] = (uint8_t)(ctx->state[i]>>24);
        digest[i*8+5] = (uint8_t)(ctx->state[i]>>16);
        digest[i*8+6] = (uint8_t)(ctx->state[i]>> 8);
        digest[i*8+7] = (uint8_t)(ctx->state[i]    );
    }
}

/* ======================================================================
 * Shared JNI helper – allocate jbyteArray from a C buffer
 * ====================================================================== */
static jbyteArray make_jbytes(JNIEnv *env, const uint8_t *buf, jsize len) {
    jbyteArray arr = (*env)->NewByteArray(env, len);
    if (arr)
        (*env)->SetByteArrayRegion(env, arr, 0, len, (const jbyte *)buf);
    return arr;
}

/* ======================================================================
 * JNI: computeMD5
 * ====================================================================== */
JNIEXPORT jbyteArray JNICALL
Java_CryptoHash_computeMD5(JNIEnv *env, jobject obj, jbyteArray data) {
    (void)obj;
    jsize  len  = (*env)->GetArrayLength(env, data);
    jbyte *buf  = (*env)->GetByteArrayElements(env, data, NULL);
    if (!buf) return NULL;

    MD5_CTX ctx;
    uint8_t digest[MD5_DIGEST_LEN];
    md5_init(&ctx);
    md5_update(&ctx, (const uint8_t *)buf, (size_t)len);
    md5_final(&ctx, digest);

    (*env)->ReleaseByteArrayElements(env, data, buf, JNI_ABORT);
    return make_jbytes(env, digest, MD5_DIGEST_LEN);
}

/* ======================================================================
 * JNI: computeSHA1
 * ====================================================================== */
JNIEXPORT jbyteArray JNICALL
Java_CryptoHash_computeSHA1(JNIEnv *env, jobject obj, jbyteArray data) {
    (void)obj;
    jsize  len  = (*env)->GetArrayLength(env, data);
    jbyte *buf  = (*env)->GetByteArrayElements(env, data, NULL);
    if (!buf) return NULL;

    SHA1_CTX ctx;
    uint8_t digest[SHA1_DIGEST_LEN];
    sha1_init(&ctx);
    sha1_update(&ctx, (const uint8_t *)buf, (size_t)len);
    sha1_final(&ctx, digest);

    (*env)->ReleaseByteArrayElements(env, data, buf, JNI_ABORT);
    return make_jbytes(env, digest, SHA1_DIGEST_LEN);
}

/* ======================================================================
 * JNI: computeSHA256
 * ====================================================================== */
JNIEXPORT jbyteArray JNICALL
Java_CryptoHash_computeSHA256(JNIEnv *env, jobject obj, jbyteArray data) {
    (void)obj;
    jsize  len  = (*env)->GetArrayLength(env, data);
    jbyte *buf  = (*env)->GetByteArrayElements(env, data, NULL);
    if (!buf) return NULL;

    SHA256_CTX ctx;
    uint8_t digest[SHA256_DIGEST_LEN];
    sha256_init(&ctx);
    sha256_update(&ctx, (const uint8_t *)buf, (size_t)len);
    sha256_final(&ctx, digest);

    (*env)->ReleaseByteArrayElements(env, data, buf, JNI_ABORT);
    return make_jbytes(env, digest, SHA256_DIGEST_LEN);
}

/* ======================================================================
 * JNI: computeSHA512
 * ====================================================================== */
JNIEXPORT jbyteArray JNICALL
Java_CryptoHash_computeSHA512(JNIEnv *env, jobject obj, jbyteArray data) {
    (void)obj;
    jsize  len  = (*env)->GetArrayLength(env, data);
    jbyte *buf  = (*env)->GetByteArrayElements(env, data, NULL);
    if (!buf) return NULL;

    SHA512_CTX ctx;
    uint8_t digest[SHA512_DIGEST_LEN];
    sha512_init(&ctx);
    sha512_update(&ctx, (const uint8_t *)buf, (size_t)len);
    sha512_final(&ctx, digest);

    (*env)->ReleaseByteArrayElements(env, data, buf, JNI_ABORT);
    return make_jbytes(env, digest, SHA512_DIGEST_LEN);
}

/* ======================================================================
 * JNI: getNativeLibraryInfo
 * ====================================================================== */
JNIEXPORT jstring JNICALL
Java_CryptoHash_getNativeLibraryInfo(JNIEnv *env, jobject obj) {
    (void)obj;
    return (*env)->NewStringUTF(env,
        "CryptoHash Native Library v1.0.0 | "
        "Algorithms: MD5 (RFC 1321), SHA-1, SHA-256, SHA-512 (FIPS 180-4) | "
        "Pure ANSI C – no third-party deps");
}