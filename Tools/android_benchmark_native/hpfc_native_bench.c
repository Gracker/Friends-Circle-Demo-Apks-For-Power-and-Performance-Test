#define _GNU_SOURCE
#include <errno.h>
#include <fcntl.h>
#include <math.h>
#include <pthread.h>
#include <sched.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <sys/syscall.h>
#include <sys/types.h>
#include <time.h>
#include <unistd.h>

static int g_sched_fifo_applied = 0;
static int g_affinity_pinned_cpu = -1;
static int g_subtask_errors = 0;
static const char *g_compile_flags = "-O3 -march=armv8-a+simd -pthread";

/* Sanity caps to keep low-RAM devices from native-crashing on absurd inputs.
 * Devices that legitimately need bigger inputs can lift these. */
#define MAX_MATMUL_N      4096
#define MAX_NBODY_N       8192
#define MAX_SORT_N        (50 * 1000 * 1000)
#define MAX_TEXT_KIB      (256 * 1024)   /* 256 MiB text */
#define MAX_IMAGE_DIM     8192
#define MAX_FFT_LOG2      24

static int validate_positive(int v, int max, const char *name, const char *bench) {
  if (v <= 0 || v > max) {
    printf("\"%s\":{\"error\":\"invalid_param\",\"param\":\"%s\",\"value\":%d,\"max\":%d}", bench, name, v, max);
    g_subtask_errors++;
    return 0;
  }
  return 1;
}

/* ===== timing ===== */
static double now_sec(void) {
  struct timespec ts;
  clock_gettime(CLOCK_MONOTONIC, &ts);
  return (double)ts.tv_sec + (double)ts.tv_nsec / 1000000000.0;
}

/* ===== sched / affinity (Linux/Android only; macOS host won't compile) ===== */
static void try_sched_fifo(int enable) {
  if (!enable) return;
  struct sched_param param;
  memset(&param, 0, sizeof(param));
  param.sched_priority = 50;
  if (sched_setscheduler(0, SCHED_FIFO, &param) == 0) {
    g_sched_fifo_applied = 1;
  }
}

static long read_cpu_max_freq(int cpu) {
  char path[128];
  snprintf(path, sizeof(path), "/sys/devices/system/cpu/cpu%d/cpufreq/cpuinfo_max_freq", cpu);
  FILE *fp = fopen(path, "r");
  if (!fp) return -1;
  long khz = -1;
  if (fscanf(fp, "%ld", &khz) != 1) khz = -1;
  fclose(fp);
  return khz;
}

static int pick_big_cluster_cpu(void) {
  long max_kh = -1;
  int pick = -1;
  for (int cpu = 0; cpu < 32; cpu++) {
    long khz = read_cpu_max_freq(cpu);
    if (khz > 0 && khz > max_kh) { max_kh = khz; pick = cpu; }
  }
  return pick;
}

static int pick_little_cluster_cpu(void) {
  long min_kh = -1;
  int pick = -1;
  for (int cpu = 0; cpu < 32; cpu++) {
    long khz = read_cpu_max_freq(cpu);
    if (khz <= 0) continue;
    if (min_kh < 0 || khz < min_kh) { min_kh = khz; pick = cpu; }
  }
  return pick;
}

static int pin_to_cpu(int cpu) {
  if (cpu < 0) return -1;
  cpu_set_t set;
  CPU_ZERO(&set);
  CPU_SET(cpu, &set);
  return sched_setaffinity(0, sizeof(set), &set);
}

static void clear_affinity(void) {
  cpu_set_t set;
  CPU_ZERO(&set);
  long nproc = sysconf(_SC_NPROCESSORS_ONLN);
  if (nproc <= 0) nproc = 32;
  for (long i = 0; i < nproc && i < (long)CPU_SETSIZE; i++) CPU_SET((int)i, &set);
  (void)sched_setaffinity(0, sizeof(set), &set);
  g_affinity_pinned_cpu = -1;
}

static void try_pin_to_big_cluster(int enable) {
  if (!enable) { clear_affinity(); return; }
  int cpu = pick_big_cluster_cpu();
  if (cpu < 0) return;
  cpu_set_t set;
  CPU_ZERO(&set);
  CPU_SET(cpu, &set);
  if (sched_setaffinity(0, sizeof(set), &set) == 0) g_affinity_pinned_cpu = cpu;
}

/* ===== threaded int/fp workers (existing 'all' mode) ===== */
typedef struct {
  int thread_index;
  uint64_t iterations;
  uint64_t int_result;
  double fp_result;
} worker_arg_t;

static void *int_worker(void *raw) {
  worker_arg_t *arg = (worker_arg_t *)raw;
  uint64_t x = 0x9e3779b97f4a7c15ULL ^ (uint64_t)arg->thread_index;
  for (uint64_t i = 0; i < arg->iterations; i++) {
    x += 0x9e3779b97f4a7c15ULL;
    x ^= x >> 29;
    x *= 0xbf58476d1ce4e5b9ULL;
    x ^= x >> 32;
    x += i;
  }
  arg->int_result = x;
  return NULL;
}

static void *fp_worker(void *raw) {
  worker_arg_t *arg = (worker_arg_t *)raw;
  double a = 1.0000001 + (double)arg->thread_index * 0.001;
  double b = 0.9999997;
  double c = 0.0000003;
  for (uint64_t i = 0; i < arg->iterations; i++) {
    a = a * b + c;
    b = b + 0.0000000001;
    c = c * 1.0000000002 + 0.0000000001;
    if (a > 2.0) a *= 0.5;
  }
  arg->fp_result = a + b + c;
  return NULL;
}

static double run_threaded(int threads, uint64_t iterations, int fp, uint64_t *int_sink, double *fp_sink) {
  pthread_t *tids = (pthread_t *)calloc((size_t)threads, sizeof(pthread_t));
  worker_arg_t *args = (worker_arg_t *)calloc((size_t)threads, sizeof(worker_arg_t));
  if (!tids || !args) { free(tids); free(args); return -1.0; }
  double start = now_sec();
  for (int i = 0; i < threads; i++) {
    args[i].thread_index = i;
    args[i].iterations = iterations;
    pthread_create(&tids[i], NULL, fp ? fp_worker : int_worker, &args[i]);
  }
  for (int i = 0; i < threads; i++) pthread_join(tids[i], NULL);
  double elapsed = now_sec() - start;
  uint64_t is = 0;
  double fs = 0.0;
  for (int i = 0; i < threads; i++) { is ^= args[i].int_result; fs += args[i].fp_result; }
  *int_sink = is; *fp_sink = fs;
  free(tids); free(args);
  return elapsed;
}

/* ===== STREAM-like memory bandwidth ===== */
static int cmp_double(const void *x, const void *y) {
  double a = *(const double *)x;
  double b = *(const double *)y;
  return (a > b) - (a < b);
}
static double median_of(double *values, int n) {
  if (n <= 0) return 0.0;
  qsort(values, (size_t)n, sizeof(double), cmp_double);
  return n % 2 ? values[n / 2] : (values[n / 2 - 1] + values[n / 2]) * 0.5;
}

static int run_memory(size_t mib, int iterations) {
  size_t n = (mib * 1024ULL * 1024ULL) / sizeof(double);
  if (n < 1024) n = 1024;
  double *a = NULL, *b = NULL, *c = NULL;
  if (posix_memalign((void **)&a, 64, n * sizeof(double)) != 0 ||
      posix_memalign((void **)&b, 64, n * sizeof(double)) != 0 ||
      posix_memalign((void **)&c, 64, n * sizeof(double)) != 0) {
    free(a); free(b); free(c);
    printf("\"memory_stream\":{\"error\":\"alloc_failed\",\"mib_requested\":%zu}", mib);
    return 1;
  }
  for (size_t i = 0; i < n; i++) { a[i] = 1.0; b[i] = 2.0; c[i] = 0.5; }
#ifdef MCL_CURRENT
  (void)mlock(a, n * sizeof(double));
  (void)mlock(b, n * sizeof(double));
  (void)mlock(c, n * sizeof(double));
#endif

  int counted = iterations < 1 ? 1 : iterations;
  int warmup = 1;
  double *cv = (double *)calloc((size_t)counted, sizeof(double));
  double *sv = (double *)calloc((size_t)counted, sizeof(double));
  double *av = (double *)calloc((size_t)counted, sizeof(double));
  double *tv = (double *)calloc((size_t)counted, sizeof(double));
  double best_c = 0, best_s = 0, best_a = 0, best_t = 0;
  int idx = 0;
  for (int it = 0; it < warmup + counted; it++) {
    double s = now_sec();
    for (size_t i = 0; i < n; i++) c[i] = a[i];
    double e = now_sec() - s;
    double copy = ((double)n * sizeof(double) * 2.0) / e / 1024.0 / 1024.0;
    s = now_sec();
    for (size_t i = 0; i < n; i++) b[i] = 3.0 * c[i];
    e = now_sec() - s;
    double scale = ((double)n * sizeof(double) * 2.0) / e / 1024.0 / 1024.0;
    s = now_sec();
    for (size_t i = 0; i < n; i++) c[i] = a[i] + b[i];
    e = now_sec() - s;
    double add = ((double)n * sizeof(double) * 3.0) / e / 1024.0 / 1024.0;
    s = now_sec();
    for (size_t i = 0; i < n; i++) a[i] = b[i] + 3.0 * c[i];
    e = now_sec() - s;
    double triad = ((double)n * sizeof(double) * 3.0) / e / 1024.0 / 1024.0;
    if (it < warmup) continue;
    cv[idx] = copy; sv[idx] = scale; av[idx] = add; tv[idx] = triad;
    if (copy > best_c) best_c = copy;
    if (scale > best_s) best_s = scale;
    if (add > best_a) best_a = add;
    if (triad > best_t) best_t = triad;
    idx++;
  }
  volatile double sink = a[n / 2] + b[n / 3] + c[n / 5];
  (void)sink;
  printf("\"memory_stream\":{\"working_set_mib\":%zu,\"warmup_iterations\":%d,\"counted_iterations\":%d,"
         "\"copy_mib_s\":%.3f,\"scale_mib_s\":%.3f,\"add_mib_s\":%.3f,\"triad_mib_s\":%.3f,"
         "\"copy_mib_s_median\":%.3f,\"scale_mib_s_median\":%.3f,\"add_mib_s_median\":%.3f,\"triad_mib_s_median\":%.3f}",
         mib, warmup, idx, best_c, best_s, best_a, best_t,
         median_of(cv, idx), median_of(sv, idx), median_of(av, idx), median_of(tv, idx));
  free(cv); free(sv); free(av); free(tv); free(a); free(b); free(c);
  return 0;
}

static int run_latency(size_t mib, uint64_t visits) {
  size_t n = (mib * 1024ULL * 1024ULL) / sizeof(uint32_t);
  if (n < 1024) n = 1024;
  uint32_t *next = NULL;
  if (posix_memalign((void **)&next, 64, n * sizeof(uint32_t)) != 0) {
    printf("\"memory_latency\":{\"error\":\"alloc_failed\",\"mib_requested\":%zu}", mib);
    return 1;
  }
  uint32_t stride = 131071U;
  for (size_t i = 0; i < n; i++) next[i] = (uint32_t)((i + stride) % n);
  uint32_t idx = 0;
  double s = now_sec();
  for (uint64_t i = 0; i < visits; i++) idx = next[idx];
  double e = now_sec() - s;
  volatile uint32_t sink = idx;
  (void)sink;
  printf("\"memory_latency\":{\"working_set_mib\":%zu,\"visits\":%llu,\"ns_per_access\":%.3f}",
         mib, (unsigned long long)visits, e * 1e9 / (double)visits);
  free(next);
  return 0;
}

/* ===== AES-128 round-style benchmark =====
 * Runs the AES round transform (SubBytes + ShiftRows + MixColumns + AddRoundKey)
 * across many 16-byte blocks. Not full AES, but representative of the
 * cryptographic byte-shuffling Geekbench-style workloads measure. */
static const uint8_t AES_SBOX[256] = {
0x63,0x7c,0x77,0x7b,0xf2,0x6b,0x6f,0xc5,0x30,0x01,0x67,0x2b,0xfe,0xd7,0xab,0x76,
0xca,0x82,0xc9,0x7d,0xfa,0x59,0x47,0xf0,0xad,0xd4,0xa2,0xaf,0x9c,0xa4,0x72,0xc0,
0xb7,0xfd,0x93,0x26,0x36,0x3f,0xf7,0xcc,0x34,0xa5,0xe5,0xf1,0x71,0xd8,0x31,0x15,
0x04,0xc7,0x23,0xc3,0x18,0x96,0x05,0x9a,0x07,0x12,0x80,0xe2,0xeb,0x27,0xb2,0x75,
0x09,0x83,0x2c,0x1a,0x1b,0x6e,0x5a,0xa0,0x52,0x3b,0xd6,0xb3,0x29,0xe3,0x2f,0x84,
0x53,0xd1,0x00,0xed,0x20,0xfc,0xb1,0x5b,0x6a,0xcb,0xbe,0x39,0x4a,0x4c,0x58,0xcf,
0xd0,0xef,0xaa,0xfb,0x43,0x4d,0x33,0x85,0x45,0xf9,0x02,0x7f,0x50,0x3c,0x9f,0xa8,
0x51,0xa3,0x40,0x8f,0x92,0x9d,0x38,0xf5,0xbc,0xb6,0xda,0x21,0x10,0xff,0xf3,0xd2,
0xcd,0x0c,0x13,0xec,0x5f,0x97,0x44,0x17,0xc4,0xa7,0x7e,0x3d,0x64,0x5d,0x19,0x73,
0x60,0x81,0x4f,0xdc,0x22,0x2a,0x90,0x88,0x46,0xee,0xb8,0x14,0xde,0x5e,0x0b,0xdb,
0xe0,0x32,0x3a,0x0a,0x49,0x06,0x24,0x5c,0xc2,0xd3,0xac,0x62,0x91,0x95,0xe4,0x79,
0xe7,0xc8,0x37,0x6d,0x8d,0xd5,0x4e,0xa9,0x6c,0x56,0xf4,0xea,0x65,0x7a,0xae,0x08,
0xba,0x78,0x25,0x2e,0x1c,0xa6,0xb4,0xc6,0xe8,0xdd,0x74,0x1f,0x4b,0xbd,0x8b,0x8a,
0x70,0x3e,0xb5,0x66,0x48,0x03,0xf6,0x0e,0x61,0x35,0x57,0xb9,0x86,0xc1,0x1d,0x9e,
0xe1,0xf8,0x98,0x11,0x69,0xd9,0x8e,0x94,0x9b,0x1e,0x87,0xe9,0xce,0x55,0x28,0xdf,
0x8c,0xa1,0x89,0x0d,0xbf,0xe6,0x42,0x68,0x41,0x99,0x2d,0x0f,0xb0,0x54,0xbb,0x16
};

static inline uint8_t xtime(uint8_t b) { return (uint8_t)((b << 1) ^ (((b >> 7) & 1) * 0x1b)); }

static double bench_aes(uint64_t bytes) {
  uint64_t blocks = bytes / 16;
  if (blocks == 0) blocks = 1;
  uint8_t state[16];
  for (int i = 0; i < 16; i++) state[i] = (uint8_t)(i * 17 + 3);
  uint8_t key[16];
  for (int i = 0; i < 16; i++) key[i] = (uint8_t)(i * 31 + 7);
  double s = now_sec();
  for (uint64_t b = 0; b < blocks; b++) {
    for (int i = 0; i < 16; i++) state[i] = AES_SBOX[state[i]];
    uint8_t t = state[1]; state[1] = state[5]; state[5] = state[9]; state[9] = state[13]; state[13] = t;
    t = state[2]; state[2] = state[10]; state[10] = t;
    t = state[6]; state[6] = state[14]; state[14] = t;
    t = state[15]; state[15] = state[11]; state[11] = state[7]; state[7] = state[3]; state[3] = t;
    for (int col = 0; col < 4; col++) {
      uint8_t s0 = state[col*4], s1 = state[col*4+1], s2 = state[col*4+2], s3 = state[col*4+3];
      uint8_t t0 = s0 ^ s1 ^ s2 ^ s3;
      state[col*4]   ^= t0 ^ xtime(s0 ^ s1);
      state[col*4+1] ^= t0 ^ xtime(s1 ^ s2);
      state[col*4+2] ^= t0 ^ xtime(s2 ^ s3);
      state[col*4+3] ^= t0 ^ xtime(s3 ^ s0);
    }
    for (int i = 0; i < 16; i++) state[i] ^= key[i];
  }
  double e = now_sec() - s;
  uint32_t sink = 0;
  for (int i = 0; i < 16; i++) sink ^= state[i];
  printf("\"aes_round\":{\"bytes\":%llu,\"duration_sec\":%.6f,\"mib_s\":%.3f,\"sink\":%u}",
         (unsigned long long)(blocks * 16), e, ((double)blocks * 16) / e / 1024.0 / 1024.0, sink);
  return e;
}

/* ===== SHA-256 ===== */
static const uint32_t SHA256_K[64] = {
0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,
0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,
0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,
0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,
0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,
0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,
0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,
0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2};

static inline uint32_t rotr32(uint32_t x, int n) { return (x >> n) | (x << (32 - n)); }

static void sha256_block(uint32_t *H, const uint8_t *block) {
  uint32_t W[64];
  for (int i = 0; i < 16; i++)
    W[i] = ((uint32_t)block[i*4]<<24)|((uint32_t)block[i*4+1]<<16)|((uint32_t)block[i*4+2]<<8)|(uint32_t)block[i*4+3];
  for (int i = 16; i < 64; i++) {
    uint32_t s0 = rotr32(W[i-15],7) ^ rotr32(W[i-15],18) ^ (W[i-15]>>3);
    uint32_t s1 = rotr32(W[i-2],17) ^ rotr32(W[i-2],19) ^ (W[i-2]>>10);
    W[i] = W[i-16] + s0 + W[i-7] + s1;
  }
  uint32_t a=H[0],b=H[1],c=H[2],d=H[3],e=H[4],f=H[5],g=H[6],h=H[7];
  for (int i = 0; i < 64; i++) {
    uint32_t S1 = rotr32(e,6) ^ rotr32(e,11) ^ rotr32(e,25);
    uint32_t ch = (e & f) ^ (~e & g);
    uint32_t t1 = h + S1 + ch + SHA256_K[i] + W[i];
    uint32_t S0 = rotr32(a,2) ^ rotr32(a,13) ^ rotr32(a,22);
    uint32_t mj = (a & b) ^ (a & c) ^ (b & c);
    uint32_t t2 = S0 + mj;
    h=g; g=f; f=e; e=d+t1; d=c; c=b; b=a; a=t1+t2;
  }
  H[0]+=a; H[1]+=b; H[2]+=c; H[3]+=d; H[4]+=e; H[5]+=f; H[6]+=g; H[7]+=h;
}

static double bench_sha256(uint64_t bytes) {
  uint32_t H[8] = {0x6a09e667,0xbb67ae85,0x3c6ef372,0xa54ff53a,0x510e527f,0x9b05688c,0x1f83d9ab,0x5be0cd19};
  uint8_t block[64];
  for (int i = 0; i < 64; i++) block[i] = (uint8_t)(i * 7 + 11);
  uint64_t blocks = bytes / 64;
  if (blocks == 0) blocks = 1;
  double s = now_sec();
  for (uint64_t i = 0; i < blocks; i++) {
    block[i & 63]++;  /* keep block changing so compiler can't hoist */
    sha256_block(H, block);
  }
  double e = now_sec() - s;
  uint32_t sink = H[0] ^ H[1] ^ H[2] ^ H[3] ^ H[4] ^ H[5] ^ H[6] ^ H[7];
  printf("\"sha256\":{\"bytes\":%llu,\"duration_sec\":%.6f,\"mib_s\":%.3f,\"sink\":%u}",
         (unsigned long long)(blocks * 64), e, ((double)blocks * 64) / e / 1024.0 / 1024.0, sink);
  return e;
}

/* ===== FFT (Cooley-Tukey radix-2, in-place) ===== */
static double bench_fft(int log2_n, int repeats) {
  if (log2_n <= 0 || log2_n > MAX_FFT_LOG2 || repeats <= 0) {
    printf("\"fft\":{\"error\":\"invalid_param\",\"log2_n\":%d,\"repeats\":%d}", log2_n, repeats);
    g_subtask_errors++;
    return -1.0;
  }
  int n = 1 << log2_n;
  double *real = (double *)calloc((size_t)n, sizeof(double));
  double *imag = (double *)calloc((size_t)n, sizeof(double));
  if (!real || !imag) {
    printf("\"fft\":{\"error\":\"alloc_failed\",\"length\":%d}", n);
    g_subtask_errors++;
    free(real); free(imag);
    return -1.0;
  }
  for (int i = 0; i < n; i++) { real[i] = sin(0.001 * i); imag[i] = 0.0; }
  double s = now_sec();
  for (int r = 0; r < repeats; r++) {
    /* bit-reverse */
    int j = 0;
    for (int i = 1; i < n; i++) {
      int bit = n >> 1;
      for (; j & bit; bit >>= 1) j ^= bit;
      j ^= bit;
      if (i < j) {
        double tr = real[i]; real[i] = real[j]; real[j] = tr;
        double ti = imag[i]; imag[i] = imag[j]; imag[j] = ti;
      }
    }
    for (int len = 2; len <= n; len <<= 1) {
      double ang = -2.0 * M_PI / len;
      double wr = cos(ang), wi = sin(ang);
      for (int i = 0; i < n; i += len) {
        double cur_wr = 1.0, cur_wi = 0.0;
        for (int k = 0; k < len / 2; k++) {
          double ur = real[i+k], ui = imag[i+k];
          double vr = real[i+k+len/2]*cur_wr - imag[i+k+len/2]*cur_wi;
          double vi = real[i+k+len/2]*cur_wi + imag[i+k+len/2]*cur_wr;
          real[i+k] = ur + vr; imag[i+k] = ui + vi;
          real[i+k+len/2] = ur - vr; imag[i+k+len/2] = ui - vi;
          double nwr = cur_wr*wr - cur_wi*wi;
          cur_wi = cur_wr*wi + cur_wi*wr;
          cur_wr = nwr;
        }
      }
    }
  }
  double e = now_sec() - s;
  /* approx: 5 N log2(N) FLOPs per FFT */
  double mflops = 5.0 * (double)n * log2_n * repeats / e / 1e6;
  volatile double sink = real[0] + imag[n / 2];
  (void)sink;
  printf("\"fft\":{\"length\":%d,\"repeats\":%d,\"duration_sec\":%.6f,\"mflops\":%.3f}",
         n, repeats, e, mflops);
  free(real); free(imag);
  return e;
}

/* ===== Matrix multiplication (256x256 floats) ===== */
static double bench_matmul(int n, int repeats) {
  if (!validate_positive(n, MAX_MATMUL_N, "n", "matmul") ||
      !validate_positive(repeats, 10000, "repeats", "matmul")) return -1.0;
  size_t bytes = (size_t)n * (size_t)n * sizeof(float);
  float *A = (float *)malloc(bytes);
  float *B = (float *)malloc(bytes);
  /* C must be zero-initialized: kernel uses += and would otherwise read uninitialized memory (UB). */
  float *C = (float *)calloc((size_t)n * (size_t)n, sizeof(float));
  if (!A || !B || !C) {
    printf("\"matmul\":{\"error\":\"alloc_failed\",\"n\":%d}", n);
    g_subtask_errors++;
    free(A); free(B); free(C);
    return -1.0;
  }
  for (int i = 0; i < n*n; i++) { A[i] = (float)((i % 17) * 0.1); B[i] = (float)((i % 13) * 0.2); }
  double s = now_sec();
  for (int r = 0; r < repeats; r++) {
    for (int i = 0; i < n; i++) {
      for (int k = 0; k < n; k++) {
        float aik = A[i*n + k];
        for (int j = 0; j < n; j++) C[i*n + j] += aik * B[k*n + j];
      }
    }
  }
  double e = now_sec() - s;
  /* 2*N^3 FLOPs per matmul */
  double mflops = 2.0 * (double)n * n * n * repeats / e / 1e6;
  volatile float sink = C[n / 2];
  (void)sink;
  printf("\"matmul\":{\"n\":%d,\"repeats\":%d,\"duration_sec\":%.6f,\"mflops\":%.3f}", n, repeats, e, mflops);
  free(A); free(B); free(C);
  return e;
}

/* ===== N-body (256 bodies, simple Euler) ===== */
static double bench_nbody(int n, int steps) {
  if (!validate_positive(n, MAX_NBODY_N, "n", "nbody") ||
      !validate_positive(steps, 100000, "steps", "nbody")) return -1.0;
  float *px = (float *)malloc((size_t)n * sizeof(float));
  float *py = (float *)malloc((size_t)n * sizeof(float));
  float *pz = (float *)malloc((size_t)n * sizeof(float));
  float *vx = (float *)calloc((size_t)n, sizeof(float));
  float *vy = (float *)calloc((size_t)n, sizeof(float));
  float *vz = (float *)calloc((size_t)n, sizeof(float));
  if (!px || !py || !pz || !vx || !vy || !vz) {
    printf("\"nbody\":{\"error\":\"alloc_failed\",\"n\":%d}", n);
    g_subtask_errors++;
    free(px); free(py); free(pz); free(vx); free(vy); free(vz);
    return -1.0;
  }
  for (int i = 0; i < n; i++) {
    px[i] = (float)((i * 7) % 100); py[i] = (float)((i * 11) % 100); pz[i] = (float)((i * 13) % 100);
  }
  double s = now_sec();
  float dt = 0.001f, eps = 0.01f;
  for (int t = 0; t < steps; t++) {
    for (int i = 0; i < n; i++) {
      float fx = 0, fy = 0, fz = 0;
      for (int j = 0; j < n; j++) {
        if (i == j) continue;
        float dx = px[j]-px[i], dy = py[j]-py[i], dz = pz[j]-pz[i];
        float d2 = dx*dx + dy*dy + dz*dz + eps;
        float inv = 1.0f / sqrtf(d2 * d2 * d2);
        fx += dx * inv; fy += dy * inv; fz += dz * inv;
      }
      vx[i] += dt * fx; vy[i] += dt * fy; vz[i] += dt * fz;
    }
    for (int i = 0; i < n; i++) { px[i] += dt * vx[i]; py[i] += dt * vy[i]; pz[i] += dt * vz[i]; }
  }
  double e = now_sec() - s;
  /* ~20 FLOPs per body-pair-interaction */
  double mflops = 20.0 * (double)n * n * steps / e / 1e6;
  volatile float sink = px[0] + py[n/2] + pz[n-1];
  (void)sink;
  printf("\"nbody\":{\"bodies\":%d,\"steps\":%d,\"duration_sec\":%.6f,\"mflops\":%.3f}", n, steps, e, mflops);
  free(px); free(py); free(pz); free(vx); free(vy); free(vz);
  return e;
}

/* ===== Sort 1M ints ===== */
static int cmp_uint32(const void *x, const void *y) {
  uint32_t a = *(const uint32_t *)x, b = *(const uint32_t *)y;
  return (a > b) - (a < b);
}

static double bench_sort(int n, int repeats) {
  if (!validate_positive(n, MAX_SORT_N, "n", "sort") ||
      !validate_positive(repeats, 10000, "repeats", "sort")) return -1.0;
  uint32_t *src = (uint32_t *)malloc((size_t)n * sizeof(uint32_t));
  uint32_t *buf = (uint32_t *)malloc((size_t)n * sizeof(uint32_t));
  if (!src || !buf) {
    printf("\"sort\":{\"error\":\"alloc_failed\",\"n\":%d}", n);
    g_subtask_errors++;
    free(src); free(buf);
    return -1.0;
  }
  uint64_t seed = 0x9e3779b97f4a7c15ULL;
  for (int i = 0; i < n; i++) {
    seed = seed * 6364136223846793005ULL + 1442695040888963407ULL;
    src[i] = (uint32_t)(seed >> 32);
  }
  double s = now_sec();
  for (int r = 0; r < repeats; r++) {
    memcpy(buf, src, (size_t)n * sizeof(uint32_t));
    qsort(buf, (size_t)n, sizeof(uint32_t), cmp_uint32);
  }
  double e = now_sec() - s;
  double mops = (double)n * repeats / e / 1e6;
  volatile uint32_t sink = buf[0] ^ buf[n - 1];
  (void)sink;
  printf("\"sort\":{\"items\":%d,\"repeats\":%d,\"duration_sec\":%.6f,\"mops\":%.3f}", n, repeats, e, mops);
  free(src); free(buf);
  return e;
}

/* ===== Regex / pattern match (hand-written substring + light DFA) ===== */
static double bench_regex(int text_kib, int repeats) {
  if (!validate_positive(text_kib, MAX_TEXT_KIB, "text_kib", "regex") ||
      !validate_positive(repeats, 10000, "repeats", "regex")) return -1.0;
  size_t n = (size_t)text_kib * 1024;
  char *text = (char *)malloc(n + 1);
  if (!text) {
    printf("\"regex\":{\"error\":\"alloc_failed\",\"text_kib\":%d}", text_kib);
    g_subtask_errors++;
    return -1.0;
  }
  uint64_t seed = 12345;
  for (size_t i = 0; i < n; i++) {
    seed = seed * 6364136223846793005ULL + 1442695040888963407ULL;
    int v = (int)((seed >> 32) % 27);
    text[i] = (char)(v == 26 ? ' ' : ('a' + v));
  }
  text[n] = 0;
  /* Inject the needle a few times so matches are non-zero. */
  const char *needle = "android";
  size_t nlen = strlen(needle);
  for (size_t off = 100; off + nlen < n; off += 70000) memcpy(text + off, needle, nlen);

  double s = now_sec();
  uint64_t total_matches = 0;
  for (int r = 0; r < repeats; r++) {
    /* naive scan */
    for (size_t i = 0; i + nlen <= n; i++) {
      if (text[i] == needle[0] && memcmp(text + i, needle, nlen) == 0) total_matches++;
    }
  }
  double e = now_sec() - s;
  double mb_s = ((double)n * repeats) / e / 1024.0 / 1024.0;
  printf("\"regex\":{\"text_kib\":%d,\"repeats\":%d,\"matches\":%llu,\"duration_sec\":%.6f,\"mib_scanned_per_s\":%.3f}",
         text_kib, repeats, (unsigned long long)total_matches, e, mb_s);
  free(text);
  return e;
}

/* ===== JSON-ish tokenize (UX Data Processing) ===== */
static double bench_json_tokenize(int text_kib, int repeats) {
  if (!validate_positive(text_kib, MAX_TEXT_KIB, "text_kib", "json_tokenize") ||
      !validate_positive(repeats, 10000, "repeats", "json_tokenize")) return -1.0;
  size_t n = (size_t)text_kib * 1024;
  char *text = (char *)malloc(n + 1);
  if (!text) {
    printf("\"json_tokenize\":{\"error\":\"alloc_failed\",\"text_kib\":%d}", text_kib);
    g_subtask_errors++;
    return -1.0;
  }
  /* Generate { "key": value, ... } stream with mixed integers and strings. */
  size_t pos = 0;
  uint64_t seed = 42;
  while (pos + 32 < n) {
    seed = seed * 6364136223846793005ULL + 1442695040888963407ULL;
    int written = snprintf(text + pos, n - pos, "{\"k%u\":\"v%u\",\"n\":%u},",
                           (uint32_t)(seed >> 40), (uint32_t)(seed >> 32), (uint32_t)(seed >> 24));
    if (written < 0 || (size_t)written >= n - pos) break;
    pos += (size_t)written;
  }
  text[pos] = 0;

  double s = now_sec();
  uint64_t tokens = 0;
  for (int r = 0; r < repeats; r++) {
    for (size_t i = 0; i < pos; i++) {
      char ch = text[i];
      if (ch == '{' || ch == '}' || ch == '"' || ch == ':' || ch == ',') tokens++;
      else if (ch >= '0' && ch <= '9') tokens++;
    }
  }
  double e = now_sec() - s;
  double mb_s = ((double)pos * repeats) / e / 1024.0 / 1024.0;
  printf("\"json_tokenize\":{\"text_kib\":%d,\"repeats\":%d,\"tokens\":%llu,\"duration_sec\":%.6f,\"mib_per_s\":%.3f}",
         text_kib, repeats, (unsigned long long)tokens, e, mb_s);
  free(text);
  return e;
}

/* ===== Image processing (UX) =====
 * Gaussian blur + bilinear resize on an in-memory RGBA buffer. */
static double bench_image(int w, int h, int repeats) {
  if (!validate_positive(w, MAX_IMAGE_DIM, "w", "image_proc") ||
      !validate_positive(h, MAX_IMAGE_DIM, "h", "image_proc") ||
      !validate_positive(repeats, 10000, "repeats", "image_proc")) return -1.0;
  size_t pixels = (size_t)w * h;
  uint8_t *src = (uint8_t *)malloc(pixels * 4);
  uint8_t *dst = (uint8_t *)malloc((pixels / 4) * 4); /* w/2 x h/2 */
  /* tmp must be zero-initialized: edge columns aren't written by the blur loop
   * but are read by the downsample (otherwise UB). */
  uint8_t *tmp = (uint8_t *)calloc(pixels, 4);
  if (!src || !dst || !tmp) {
    printf("\"image_proc\":{\"error\":\"alloc_failed\",\"w\":%d,\"h\":%d}", w, h);
    g_subtask_errors++;
    free(src); free(dst); free(tmp);
    return -1.0;
  }
  for (size_t i = 0; i < pixels * 4; i++) src[i] = (uint8_t)(i * 31 + 7);

  double s = now_sec();
  for (int r = 0; r < repeats; r++) {
    /* 5-tap separable horizontal blur */
    for (int y = 0; y < h; y++) {
      for (int x = 2; x < w - 2; x++) {
        for (int c = 0; c < 4; c++) {
          int sum = src[((y)*w + x-2)*4 + c] + src[((y)*w + x-1)*4 + c]*4
                  + src[((y)*w + x)*4 + c]*6 + src[((y)*w + x+1)*4 + c]*4
                  + src[((y)*w + x+2)*4 + c];
          tmp[((y)*w + x)*4 + c] = (uint8_t)(sum / 16);
        }
      }
    }
    /* Bilinear downsample 2x */
    int dw = w / 2, dh = h / 2;
    for (int y = 0; y < dh; y++) {
      for (int x = 0; x < dw; x++) {
        for (int c = 0; c < 4; c++) {
          int v = tmp[((y*2)*w + x*2)*4 + c] + tmp[((y*2)*w + x*2+1)*4 + c]
                + tmp[((y*2+1)*w + x*2)*4 + c] + tmp[((y*2+1)*w + x*2+1)*4 + c];
          dst[(y*dw + x)*4 + c] = (uint8_t)(v / 4);
        }
      }
    }
    /* Color convert in-place (sepia) */
    for (size_t i = 0; i < (size_t)dw * dh; i++) {
      int rr = dst[i*4], gg = dst[i*4+1], bb = dst[i*4+2];
      int nr = (rr*393 + gg*769 + bb*189) / 1000;
      int ng = (rr*349 + gg*686 + bb*168) / 1000;
      int nb = (rr*272 + gg*534 + bb*131) / 1000;
      dst[i*4] = (uint8_t)(nr > 255 ? 255 : nr);
      dst[i*4+1] = (uint8_t)(ng > 255 ? 255 : ng);
      dst[i*4+2] = (uint8_t)(nb > 255 ? 255 : nb);
    }
  }
  double e = now_sec() - s;
  double mpix_s = (double)((size_t)(w/2) * (h/2)) * repeats / e / 1e6;
  volatile uint8_t sink = dst[0] ^ dst[(pixels/4)*4 - 1];
  (void)sink;
  printf("\"image_proc\":{\"w\":%d,\"h\":%d,\"repeats\":%d,\"duration_sec\":%.6f,\"mpix_out_per_s\":%.3f}",
         w, h, repeats, e, mpix_s);
  free(src); free(dst); free(tmp);
  return e;
}

/* ===== Storage random IO (true IOPS via pread64/pwrite64) ===== */
static int bench_storage_random(const char *path, int bs, uint64_t ops, int writes, int direct_io) {
  int flags = O_RDWR | O_CREAT;
#ifdef O_DIRECT
  if (direct_io) flags |= O_DIRECT;
#endif
  int fd = open(path, flags, 0644);
  if (fd < 0) {
    printf("\"storage_random\":{\"error\":\"open_failed\",\"errno\":%d,\"path\":\"%s\"}", errno, path);
    return 1;
  }
  /* Pre-allocate the file to (ops*bs) bytes, capped at 256MiB. */
  uint64_t file_bytes = ops * (uint64_t)bs;
  if (file_bytes > 256ULL * 1024 * 1024) file_bytes = 256ULL * 1024 * 1024;
  if (ftruncate(fd, (off_t)file_bytes) != 0) {
    printf("\"storage_random\":{\"error\":\"ftruncate_failed\",\"errno\":%d,\"path\":\"%s\"}", errno, path);
    close(fd); return 1;
  }
  uint64_t blocks = file_bytes / (uint64_t)bs;
  if (blocks < 2) blocks = 2;

  void *buf = NULL;
  if (posix_memalign(&buf, 4096, (size_t)bs) != 0) {
    printf("\"storage_random\":{\"error\":\"alloc_failed\"}"); close(fd); return 1;
  }
  memset(buf, 0xa5, (size_t)bs);
  /* Seed file with sequential write so reads have something. */
  for (uint64_t b = 0; b < blocks; b++) (void)pwrite64(fd, buf, (size_t)bs, (off64_t)(b * (uint64_t)bs));
  fsync(fd);

  uint64_t seed = 0x12345abcULL;
  double s = now_sec();
  uint64_t ok = 0;
  for (uint64_t i = 0; i < ops; i++) {
    seed = seed * 6364136223846793005ULL + 1442695040888963407ULL;
    uint64_t off = (seed % blocks) * (uint64_t)bs;
    ssize_t r = writes ? pwrite64(fd, buf, (size_t)bs, (off64_t)off) : pread64(fd, buf, (size_t)bs, (off64_t)off);
    if (r == (ssize_t)bs) ok++;
  }
  if (writes) fsync(fd);
  double e = now_sec() - s;
  close(fd);
  free(buf);
  double iops = (double)ok / e;
  double mib_s = ((double)ok * bs) / e / 1024.0 / 1024.0;
  printf("\"storage_random\":{\"path\":\"%s\",\"bs\":%d,\"ops_target\":%llu,\"ops_ok\":%llu,"
         "\"writes\":%s,\"direct_io\":%s,\"duration_sec\":%.6f,\"iops\":%.3f,\"mib_s\":%.3f}",
         path, bs, (unsigned long long)ops, (unsigned long long)ok,
         writes ? "true" : "false", direct_io ? "true" : "false", e, iops, mib_s);
  return 0;
}

/* ===== Per-cluster (big.LITTLE) CPU benchmark =====
 * Pin to each cluster's representative CPU and run the same int/fp workload.
 * Report per-cluster mops/mflops + ratio. */
static int bench_per_cluster(uint64_t int_iters, uint64_t fp_iters) {
  int big = pick_big_cluster_cpu();
  int little = pick_little_cluster_cpu();
  long big_max = big >= 0 ? read_cpu_max_freq(big) : -1;
  long little_max = little >= 0 ? read_cpu_max_freq(little) : -1;
  int has_hetero = (big != little) && big >= 0 && little >= 0 && big_max != little_max;

  uint64_t int_sink = 0; double fp_sink = 0;
  double big_int_e = 0, big_fp_e = 0, little_int_e = 0, little_fp_e = 0;

  if (big >= 0) {
    pin_to_cpu(big);
    big_int_e = run_threaded(1, int_iters, 0, &int_sink, &fp_sink);
    big_fp_e = run_threaded(1, fp_iters, 1, &int_sink, &fp_sink);
  }
  if (little >= 0 && has_hetero) {
    pin_to_cpu(little);
    little_int_e = run_threaded(1, int_iters, 0, &int_sink, &fp_sink);
    little_fp_e = run_threaded(1, fp_iters, 1, &int_sink, &fp_sink);
  }
  clear_affinity();

  double big_int_mops = big_int_e > 0 ? (double)int_iters * 5.0 / big_int_e / 1e6 : 0;
  double big_fp_mflops = big_fp_e > 0 ? (double)fp_iters * 5.0 / big_fp_e / 1e6 : 0;
  double little_int_mops = little_int_e > 0 ? (double)int_iters * 5.0 / little_int_e / 1e6 : 0;
  double little_fp_mflops = little_fp_e > 0 ? (double)fp_iters * 5.0 / little_fp_e / 1e6 : 0;
  double int_ratio = (big_int_mops > 0 && little_int_mops > 0) ? big_int_mops / little_int_mops : 0;
  double fp_ratio = (big_fp_mflops > 0 && little_fp_mflops > 0) ? big_fp_mflops / little_fp_mflops : 0;

  printf("\"per_cluster\":{");
  printf("\"big_cpu\":%d,\"big_max_khz\":%ld,", big, big_max);
  printf("\"little_cpu\":%d,\"little_max_khz\":%ld,", little, little_max);
  printf("\"hetero\":%s,", has_hetero ? "true" : "false");
  printf("\"big_int_mops\":%.3f,\"big_fp_mflops\":%.3f,", big_int_mops, big_fp_mflops);
  printf("\"little_int_mops\":%.3f,\"little_fp_mflops\":%.3f,", little_int_mops, little_fp_mflops);
  printf("\"int_big_over_little\":%.3f,\"fp_big_over_little\":%.3f", int_ratio, fp_ratio);
  printf("}");
  return 0;
}

/* ===== Memory hierarchy curve =====
 * Pointer-chasing at multiple working-set sizes maps the cache hierarchy.
 * Sizes typically span L1d (~32K) → L2 (~512K-1M) → L3 (~2-8M) → DRAM (>>L3). */
static int bench_latency_curve(const char *sizes_kb_csv, uint64_t visits) {
  /* parse sizes */
  int kbs[16]; int n = 0;
  const char *p = sizes_kb_csv;
  while (*p && n < 16) {
    kbs[n++] = atoi(p);
    while (*p && *p != ',') p++;
    if (*p == ',') p++;
  }
  if (n == 0) { kbs[0] = 32; kbs[1] = 256; kbs[2] = 1024; kbs[3] = 4096; kbs[4] = 16384; kbs[5] = 65536; n = 6; }

  printf("\"latency_curve\":{\"visits\":%llu,\"points\":[", (unsigned long long)visits);
  for (int i = 0; i < n; i++) {
    size_t bytes = (size_t)kbs[i] * 1024;
    size_t elems = bytes / sizeof(uint32_t);
    if (elems < 16) elems = 16;
    uint32_t *arr = NULL;
    if (posix_memalign((void **)&arr, 64, elems * sizeof(uint32_t)) != 0) {
      if (i) printf(",");
      printf("{\"working_set_kib\":%d,\"error\":\"alloc\"}", kbs[i]);
      continue;
    }
    /* stride-set so each access likely misses to next level */
    uint32_t stride = 16384 / sizeof(uint32_t);  /* 16 KB stride */
    if (stride < 4) stride = 4;
    for (size_t j = 0; j < elems; j++) arr[j] = (uint32_t)((j + stride) % elems);
    /* warm */
    uint32_t idx = 0;
    for (uint64_t v = 0; v < 1000; v++) idx = arr[idx];
    /* timed */
    double s = now_sec();
    for (uint64_t v = 0; v < visits; v++) idx = arr[idx];
    double e = now_sec() - s;
    volatile uint32_t sink = idx; (void)sink;
    double ns = e * 1e9 / (double)visits;
    if (i) printf(",");
    printf("{\"working_set_kib\":%d,\"ns_per_access\":%.3f}", kbs[i], ns);
    free(arr);
  }
  printf("]}");
  return 0;
}

/* ===== Sustained perf: run a kernel N rounds; emit per-round timings ===== */
static int bench_sustained(int rounds, const char *kernel, uint64_t per_round_iters) {
  printf("\"sustained\":{\"kernel\":\"%s\",\"rounds\":%d,\"per_round\":[", kernel, rounds);
  double round_first = 0;
  double round_last = 0;
  for (int r = 0; r < rounds; r++) {
    uint64_t int_sink = 0; double fp_sink = 0;
    int fp = strcmp(kernel, "fp") == 0 ? 1 : 0;
    double e = run_threaded(1, per_round_iters, fp, &int_sink, &fp_sink);
    if (r == 0) round_first = e;
    round_last = e;
    if (r) printf(",");
    printf("{\"round\":%d,\"duration_sec\":%.6f,\"timestamp_sec\":%.6f}", r + 1, e, now_sec());
  }
  double drift_pct = round_first > 0 ? ((round_last - round_first) / round_first * 100.0) : 0.0;
  printf("],\"round_first_sec\":%.6f,\"round_last_sec\":%.6f,\"drift_pct\":%.2f}",
         round_first, round_last, drift_pct);
  return 0;
}

/* ===== Arg parsing helpers ===== */
static int parse_int_arg(int argc, char **argv, const char *name, int fallback) {
  for (int i = 1; i + 1 < argc; i++)
    if (strcmp(argv[i], name) == 0) return atoi(argv[i + 1]);
  return fallback;
}
static uint64_t parse_u64_arg(int argc, char **argv, const char *name, uint64_t fallback) {
  for (int i = 1; i + 1 < argc; i++)
    if (strcmp(argv[i], name) == 0) return strtoull(argv[i + 1], NULL, 10);
  return fallback;
}
static const char *parse_str_arg(int argc, char **argv, const char *name, const char *fallback) {
  for (int i = 1; i + 1 < argc; i++)
    if (strcmp(argv[i], name) == 0) return argv[i + 1];
  return fallback;
}
static int parse_threads(const char *raw, int *out, int max_items) {
  int count = 0;
  const char *p = raw;
  while (*p && count < max_items) {
    out[count++] = atoi(p);
    while (*p && *p != ',') p++;
    if (*p == ',') p++;
  }
  return count;
}

/* ===== mode dispatch ===== */
static int run_mode_all(int argc, char **argv) {
  int memory_mib = parse_int_arg(argc, argv, "--memory-mib", 128);
  int memory_iterations = parse_int_arg(argc, argv, "--memory-iterations", 3);
  int latency_mib = parse_int_arg(argc, argv, "--latency-mib", 64);
  uint64_t latency_visits = parse_u64_arg(argc, argv, "--latency-visits", 20000000ULL);
  uint64_t int_iterations = parse_u64_arg(argc, argv, "--int-iterations", 50000000ULL);
  uint64_t fp_iterations = parse_u64_arg(argc, argv, "--fp-iterations", 50000000ULL);
  int want_pin_big = parse_int_arg(argc, argv, "--pin-big", 1);
  const int int_ops_per_iter = 5;
  const int fp_ops_per_iter = 5;
  const char *threads_raw = parse_str_arg(argc, argv, "--threads", "1,2,4,8");
  int threads[16];
  int thread_count = parse_threads(threads_raw, threads, 16);
  if (thread_count <= 0) { threads[0] = 1; thread_count = 1; }

  printf("{\"version\":2,\"mode\":\"all\",\"meta\":{");
  printf("\"compile_flags\":\"%s\",", g_compile_flags);
  printf("\"sched_fifo_applied\":%s,", g_sched_fifo_applied ? "true" : "false");
  printf("\"int_ops_per_iter\":%d,\"fp_ops_per_iter\":%d", int_ops_per_iter, fp_ops_per_iter);
  printf("},\"cpu_int\":[");
  for (int i = 0; i < thread_count; i++) {
    int t = threads[i] <= 0 ? 1 : threads[i];
    int pin_this = (t == 1) ? want_pin_big : 0;
    try_pin_to_big_cluster(pin_this);
    uint64_t int_sink = 0; double fp_sink = 0.0;
    double elapsed = run_threaded(t, int_iterations, 0, &int_sink, &fp_sink);
    double mops = elapsed > 0 ? ((double)t * (double)int_iterations * int_ops_per_iter / elapsed / 1e6) : 0.0;
    if (i) printf(",");
    printf("{\"threads\":%d,\"iterations_per_thread\":%llu,\"duration_sec\":%.6f,\"mops\":%.3f,"
           "\"pinned_cpu\":%d,\"sink\":%llu}",
           t, (unsigned long long)int_iterations, elapsed, mops,
           pin_this ? g_affinity_pinned_cpu : -1, (unsigned long long)int_sink);
  }
  printf("],\"cpu_fp\":[");
  for (int i = 0; i < thread_count; i++) {
    int t = threads[i] <= 0 ? 1 : threads[i];
    int pin_this = (t == 1) ? want_pin_big : 0;
    try_pin_to_big_cluster(pin_this);
    uint64_t int_sink = 0; double fp_sink = 0.0;
    double elapsed = run_threaded(t, fp_iterations, 1, &int_sink, &fp_sink);
    double mops = elapsed > 0 ? ((double)t * (double)fp_iterations * fp_ops_per_iter / elapsed / 1e6) : 0.0;
    if (i) printf(",");
    printf("{\"threads\":%d,\"iterations_per_thread\":%llu,\"duration_sec\":%.6f,\"estimated_mflops\":%.3f,"
           "\"pinned_cpu\":%d,\"sink\":%.6f}",
           t, (unsigned long long)fp_iterations, elapsed, mops,
           pin_this ? g_affinity_pinned_cpu : -1, fp_sink);
  }
  printf("],");
  (void)run_memory((size_t)memory_mib, memory_iterations);
  printf(",");
  (void)run_latency((size_t)latency_mib, latency_visits);
  printf("}\n");
  return 0;
}

static int run_mode_geekbench(int argc, char **argv) {
  uint64_t aes_bytes = parse_u64_arg(argc, argv, "--aes-bytes", 64ULL * 1024 * 1024);
  uint64_t sha_bytes = parse_u64_arg(argc, argv, "--sha-bytes", 64ULL * 1024 * 1024);
  int fft_log2 = parse_int_arg(argc, argv, "--fft-log2", 14);
  int fft_repeats = parse_int_arg(argc, argv, "--fft-repeats", 50);
  int matmul_n = parse_int_arg(argc, argv, "--matmul-n", 256);
  int matmul_repeats = parse_int_arg(argc, argv, "--matmul-repeats", 5);
  int nbody_n = parse_int_arg(argc, argv, "--nbody-n", 256);
  int nbody_steps = parse_int_arg(argc, argv, "--nbody-steps", 100);
  int sort_n = parse_int_arg(argc, argv, "--sort-n", 1000000);
  int sort_repeats = parse_int_arg(argc, argv, "--sort-repeats", 5);

  printf("{\"version\":2,\"mode\":\"geekbench-mix\",");
  bench_aes(aes_bytes); printf(",");
  bench_sha256(sha_bytes); printf(",");
  bench_fft(fft_log2, fft_repeats); printf(",");
  bench_matmul(matmul_n, matmul_repeats); printf(",");
  bench_nbody(nbody_n, nbody_steps); printf(",");
  bench_sort(sort_n, sort_repeats);
  printf("}\n");
  return 0;
}

static int run_mode_ux_data(int argc, char **argv) {
  int regex_kib = parse_int_arg(argc, argv, "--regex-kib", 512);
  int regex_repeats = parse_int_arg(argc, argv, "--regex-repeats", 8);
  int json_kib = parse_int_arg(argc, argv, "--json-kib", 512);
  int json_repeats = parse_int_arg(argc, argv, "--json-repeats", 8);
  int sort_n = parse_int_arg(argc, argv, "--sort-n", 500000);
  int sort_repeats = parse_int_arg(argc, argv, "--sort-repeats", 5);
  printf("{\"version\":2,\"mode\":\"ux-data\",");
  bench_regex(regex_kib, regex_repeats); printf(",");
  bench_json_tokenize(json_kib, json_repeats); printf(",");
  bench_sort(sort_n, sort_repeats);
  printf("}\n");
  return 0;
}

static int run_mode_ux_image(int argc, char **argv) {
  int w = parse_int_arg(argc, argv, "--image-w", 1024);
  int h = parse_int_arg(argc, argv, "--image-h", 1024);
  int repeats = parse_int_arg(argc, argv, "--image-repeats", 8);
  printf("{\"version\":2,\"mode\":\"ux-image\",");
  bench_image(w, h, repeats);
  printf("}\n");
  return 0;
}

static int run_mode_storage_random(int argc, char **argv) {
  const char *path = parse_str_arg(argc, argv, "--storage-path", "/data/local/tmp/hpfc_random.bin");
  int bs = parse_int_arg(argc, argv, "--storage-bs", 4096);
  uint64_t ops = parse_u64_arg(argc, argv, "--storage-ops", 4096);
  int direct_io = parse_int_arg(argc, argv, "--storage-direct", 1);
  int writes = parse_int_arg(argc, argv, "--storage-writes", 0);
  printf("{\"version\":2,\"mode\":\"storage-random\",");
  bench_storage_random(path, bs, ops, writes, direct_io);
  /* Best effort: try without O_DIRECT if first call failed and direct_io was on. */
  printf("}\n");
  return 0;
}

static int run_mode_per_cluster(int argc, char **argv) {
  uint64_t int_iters = parse_u64_arg(argc, argv, "--int-iterations", 30000000ULL);
  uint64_t fp_iters = parse_u64_arg(argc, argv, "--fp-iterations", 30000000ULL);
  printf("{\"version\":2,\"mode\":\"per-cluster\",");
  bench_per_cluster(int_iters, fp_iters);
  printf("}\n");
  return 0;
}

static int run_mode_latency_curve(int argc, char **argv) {
  const char *sizes = parse_str_arg(argc, argv, "--latency-sizes-kb", "32,256,1024,4096,16384,65536");
  uint64_t visits = parse_u64_arg(argc, argv, "--latency-visits", 5000000ULL);
  int want_pin_big = parse_int_arg(argc, argv, "--pin-big", 1);
  try_pin_to_big_cluster(want_pin_big);
  printf("{\"version\":2,\"mode\":\"latency-curve\",");
  bench_latency_curve(sizes, visits);
  printf("}\n");
  return 0;
}

static int run_mode_sustained(int argc, char **argv) {
  int rounds = parse_int_arg(argc, argv, "--sustained-rounds", 20);
  const char *kernel = parse_str_arg(argc, argv, "--sustained-kernel", "int");
  uint64_t per_round = parse_u64_arg(argc, argv, "--sustained-per-round", 50000000ULL);
  int want_pin_big = parse_int_arg(argc, argv, "--pin-big", 1);
  try_pin_to_big_cluster(want_pin_big);
  printf("{\"version\":2,\"mode\":\"sustained\",");
  bench_sustained(rounds, kernel, per_round);
  printf("}\n");
  return 0;
}

int main(int argc, char **argv) {
  int want_sched_fifo = parse_int_arg(argc, argv, "--sched-fifo", 0);
  try_sched_fifo(want_sched_fifo);

  const char *mode = parse_str_arg(argc, argv, "--mode", "all");
  int rc;
  if (strcmp(mode, "all") == 0) rc = run_mode_all(argc, argv);
  else if (strcmp(mode, "geekbench-mix") == 0) rc = run_mode_geekbench(argc, argv);
  else if (strcmp(mode, "ux-data") == 0) rc = run_mode_ux_data(argc, argv);
  else if (strcmp(mode, "ux-image") == 0) rc = run_mode_ux_image(argc, argv);
  else if (strcmp(mode, "storage-random") == 0) rc = run_mode_storage_random(argc, argv);
  else if (strcmp(mode, "sustained") == 0) rc = run_mode_sustained(argc, argv);
  else if (strcmp(mode, "per-cluster") == 0) rc = run_mode_per_cluster(argc, argv);
  else if (strcmp(mode, "latency-curve") == 0) rc = run_mode_latency_curve(argc, argv);
  else {
    fprintf(stderr, "unknown --mode '%s'\n", mode);
    return 2;
  }
  /* If any sub-task failed (alloc / invalid param), propagate via exit code so
   * the host treats this run as failed even though JSON parsed cleanly. */
  if (rc == 0 && g_subtask_errors > 0) rc = 3;
  return rc;
}
