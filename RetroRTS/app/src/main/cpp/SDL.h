#ifndef SDL_STUB_H
#define SDL_STUB_H

#include <stdint.h>

// Basic SDL types used by DOSBox
#ifndef SDLCALL
#define SDLCALL
#endif
#ifndef DECLSPEC
#define DECLSPEC
#endif

typedef uint32_t Uint32;
typedef uint16_t Uint16;
typedef uint8_t  Uint8;
typedef int32_t  Sint32;
typedef int16_t  Sint16;

typedef struct SDL_Rect {
    int16_t x, y;
    uint16_t w, h;
} SDL_Rect;

typedef struct SDL_Color {
    uint8_t r, g, b, unused;
} SDL_Color;

typedef struct SDL_Palette {
    int ncolors;
    SDL_Color *colors;
} SDL_Palette;

typedef struct SDL_PixelFormat {
    SDL_Palette *palette;
    uint8_t  BitsPerPixel;
    uint8_t  BytesPerPixel;
    uint32_t Rmask, Gmask, Bmask, Amask;
    uint8_t  Rshift, Gshift, Bshift, Ashift;
    uint8_t  Rloss, Gloss, Bloss, Aloss;
} SDL_PixelFormat;

typedef struct SDL_Surface {
    uint32_t flags;
    SDL_PixelFormat *format;
    int w, h;
    uint16_t pitch;
    void *pixels;
    int offset;
} SDL_Surface;

typedef struct SDL_mutex SDL_mutex;
typedef struct SDL_Thread SDL_Thread;
typedef struct SDL_CD SDL_CD;

#define SDL_MUTEX_TIMEDOUT 1
#define SDL_MUTEX_MAXWAIT (~(Uint32)0)

#ifdef __cplusplus
extern "C" {
#endif

// Timer
Uint32 SDL_GetTicks(void);
void SDL_Delay(Uint32 ms);

// Error handling
const char *SDL_GetError(void);

// Initialization
#define SDL_MAJOR_VERSION 1
#define SDL_MINOR_VERSION 2
#define SDL_PATCHLEVEL    15

#define SDL_VERSIONNUM(X, Y, Z)                     \
    ((X)*1000 + (Y)*100 + (Z))

#define SDL_COMPILEDVERSION \
    SDL_VERSIONNUM(SDL_MAJOR_VERSION, SDL_MINOR_VERSION, SDL_PATCHLEVEL)

#define SDL_VERSION_ATLEAST(X, Y, Z) \
    (SDL_COMPILEDVERSION >= SDL_VERSIONNUM(X, Y, Z))

#define SDL_INIT_TIMER          0x00000001
#define SDL_INIT_AUDIO          0x00000010
#define SDL_INIT_VIDEO          0x00000020
#define SDL_INIT_CDROM          0x00000100
#define SDL_INIT_JOYSTICK       0x00000200
#define SDL_INIT_NOPARACHUTE    0x00100000
#define SDL_INIT_EVENTTHREAD    0x01000000
#define SDL_INIT_EVERYTHING     0x0000FFFF

int SDL_Init(Uint32 flags);
int SDL_InitSubSystem(Uint32 flags);
void SDL_QuitSubSystem(Uint32 flags);
Uint32 SDL_WasInit(Uint32 flags);
void SDL_Quit(void);

// Mutex
SDL_mutex* SDL_CreateMutex(void);
void SDL_DestroyMutex(SDL_mutex* m);
int SDL_LockMutex(SDL_mutex* m);
int SDL_UnlockMutex(SDL_mutex* m);
#define SDL_mutexP(m) SDL_LockMutex(m)
#define SDL_mutexV(m) SDL_UnlockMutex(m)

// Audio (if needed by mixer.cpp)
void SDL_LockAudio(void);
void SDL_UnlockAudio(void);

#define AUDIO_U8        0x0008
#define AUDIO_S8        0x8008
#define AUDIO_U16LSB    0x0010
#define AUDIO_S16LSB    0x8010
#define AUDIO_U16MSB    0x1010
#define AUDIO_S16MSB    0x9010
#define AUDIO_U16       AUDIO_U16LSB
#define AUDIO_S16       AUDIO_S16LSB

#define SDL_LIL_ENDIAN  1234
#define SDL_BIG_ENDIAN  4321

#if defined(__BYTE_ORDER__) && defined(__ORDER_LITTLE_ENDIAN__)
#if __BYTE_ORDER__ == __ORDER_LITTLE_ENDIAN__
#define SDL_BYTEORDER SDL_LIL_ENDIAN
#else
#define SDL_BYTEORDER SDL_BIG_ENDIAN
#endif
#else
#define SDL_BYTEORDER SDL_LIL_ENDIAN
#endif

#if SDL_BYTEORDER == SDL_LIL_ENDIAN
#define AUDIO_U16SYS    AUDIO_U16LSB
#define AUDIO_S16SYS    AUDIO_S16LSB
#else
#define AUDIO_U16SYS    AUDIO_U16MSB
#define AUDIO_S16SYS    AUDIO_S16MSB
#endif

typedef void (SDLCALL *SDL_AudioCallback)(void *userdata, Uint8 *stream, int len);
typedef struct SDL_AudioSpec {
    int freq;
    uint16_t format;
    uint8_t channels;
    uint8_t silence;
    uint16_t samples;
    uint32_t size;
    SDL_AudioCallback callback;
    void *userdata;
} SDL_AudioSpec;

int SDL_OpenAudio(SDL_AudioSpec *desired, SDL_AudioSpec *obtained);
void SDL_CloseAudio(void);
void SDL_PauseAudio(int pause_on);

// Stubs for CDROM to satisfy headers
int SDL_CDNumDrives(void);
const char *SDL_CDName(int drive);
SDL_CD *SDL_CDOpen(int drive);
int SDL_CDStatus(SDL_CD *cdrom);
int SDL_CDPlay(SDL_CD *cdrom, int start, int length);
int SDL_CDPause(SDL_CD *cdrom);
int SDL_CDResume(SDL_CD *cdrom);
int SDL_CDStop(SDL_CD *cdrom);
void SDL_CDClose(SDL_CD *cdrom);

#ifdef __cplusplus
}
#endif

#endif // SDL_STUB_H
