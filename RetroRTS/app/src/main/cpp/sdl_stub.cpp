#include "SDL.h"
#include <time.h>
#include <unistd.h>

extern "C" {

Uint32 SDL_GetTicks(void) {
    struct timespec ts;
    clock_gettime(CLOCK_MONOTONIC, &ts);
    return (Uint32)(ts.tv_sec * 1000 + ts.tv_nsec / 1000000);
}

void SDL_Delay(Uint32 ms) {
    usleep(ms * 1000);
}

const char *SDL_GetError(void) { return ""; }

int SDL_Init(Uint32 flags) { (void)flags; return 0; }
int SDL_InitSubSystem(Uint32 flags) { (void)flags; return 0; }
void SDL_QuitSubSystem(Uint32 flags) { (void)flags; }
Uint32 SDL_WasInit(Uint32 flags) { return flags; }
void SDL_Quit(void) {}

SDL_mutex* SDL_CreateMutex(void) { return nullptr; }
void SDL_DestroyMutex(SDL_mutex* m) { (void)m; }
int SDL_LockMutex(SDL_mutex* m) { (void)m; return 0; }
int SDL_UnlockMutex(SDL_mutex* m) { (void)m; return 0; }

void SDL_LockAudio(void) {}
void SDL_UnlockAudio(void) {}

int SDL_OpenAudio(SDL_AudioSpec *desired, SDL_AudioSpec *obtained) {
    if (obtained) *obtained = *desired;
    return 0;
}
void SDL_CloseAudio(void) {}
void SDL_PauseAudio(int pause_on) { (void)pause_on; }

int SDL_CDNumDrives(void) { return 0; }
const char *SDL_CDName(int drive) { (void)drive; return ""; }
SDL_CD *SDL_CDOpen(int drive) { (void)drive; return nullptr; }
int SDL_CDStatus(SDL_CD *cdrom) { (void)cdrom; return 0; }
int SDL_CDPlay(SDL_CD *cdrom, int start, int length) { (void)cdrom; (void)start; (void)length; return 0; }
int SDL_CDPause(SDL_CD *cdrom) { (void)cdrom; return 0; }
int SDL_CDResume(SDL_CD *cdrom) { (void)cdrom; return 0; }
int SDL_CDStop(SDL_CD *cdrom) { (void)cdrom; return 0; }
void SDL_CDClose(SDL_CD *cdrom) { (void)cdrom; }

int dosbox_main_entry(int argc, char* argv[]) {
    // This is a stub for the missing sdlmain.cpp
    // Real implementation would need to initialize DOSBOX_Init()
    // and handle the main loop.
    return 0;
}

}
