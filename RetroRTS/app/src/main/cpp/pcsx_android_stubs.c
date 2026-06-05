#include <stdio.h>
#include <stdarg.h>
#include <android/log.h>

#define LOG_TAG "PCSX"

void SysPrintf(const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    __android_log_vprint(ANDROID_LOG_INFO, LOG_TAG, fmt, args);
    va_end(args);
}

void SysMessage(const char *fmt, ...) {
    va_list args;
    va_start(args, fmt);
    __android_log_vprint(ANDROID_LOG_INFO, LOG_TAG, fmt, args);
    va_end(args);
}

void *SysLoadLibrary(const char *lib) { return NULL; }
void *SysLoadSym(void *lib, const char *sym) { return NULL; }
const char *SysLibError() { return "Not supported"; }
void SysCloseLibrary(void *lib) {}
void SysRunGui() {}
void SysClose() {}

void pl_frame_limit(void) {}

// Pad stubs
unsigned char PAD1_readPort(int port) { return 0; }
unsigned char PAD2_readPort(int port) { return 0; }
unsigned char pl_gun_byte2;
void plat_trigger_vibrate(int port, int low, int high) {}
int in_keystate;
