#ifndef DOSBOX_CONFIG_H
#define DOSBOX_CONFIG_H

#define VERSION "dosbox-core-android"

/* Define to 1 to enable internal debugger, requires libcurses */
#define C_DEBUG 0

/* Define to 1 to enable screenshots, requires libpng */
#define C_SSHOT 0
/* Define to 1 to enable movie recording, requires zlib built without Z_SOLO */
#define C_SRECORD 0

/* Define to 1 to use opengl display output support */
#define C_OPENGL 0

/* Define to 1 to enable internal modem support, requires SDL_net */
#define C_MODEM 0

/* Define to 1 to enable IPX networking support, requires SDL_net */
#define C_IPX 0

#define X86         0x01
#define X86_64      0x02
#define MIPSEL      0x03
#define ARMV4LE     0x04
#define ARMV7LE     0x05
#define ARMV8LE     0x07

/* The type of cpu this host has */
#define C_TARGETCPU ARMV8LE

/* Define to 1 to use recompiling cpu core. */
#define C_DYNREC 1

/* Enable memory function inlining */
#define C_CORE_INLINE 1

/* Enable the FPU module */
#define C_FPU 1

#define DB_HAVE_CLOCK_GETTIME 1

/* Define to 1 to use a x86 assembly fpu core */
#define C_FPU_X86 0

/* Define to 1 to use a unaligned memory access */
#define C_UNALIGNED_MEMORY 1

#define GCC_ATTRIBUTE(x) __attribute__((x))
#define GCC_UNLIKELY(x) __builtin_expect((x),0)
#define GCC_LIKELY(x) __builtin_expect((x),1)

#define INLINE inline
#define DB_FASTCALL

typedef double            Real64;
/* The internal types */
typedef unsigned char     Bit8u;
typedef signed char       Bit8s;
typedef unsigned short    Bit16u;
typedef signed short      Bit16s;
typedef unsigned int      Bit32u;
typedef signed int        Bit32s;
typedef unsigned long long Bit64u;
typedef signed long long   Bit64s;

typedef Bit64u            Bitu;
typedef Bit64s            Bits;

#define sBit32t ""
#define sBit64t "ll"
#define sBit32fs(a) sBit32t #a
#define sBit64fs(a) sBit64t #a
#define sBitfs(a) sBit64t #a

#endif
