#include <jni.h>
#include <android/log.h>
#include <string>
#include <cstring>
#include <cstdio>
#include <cstdarg>
#include <thread>
#include <atomic>
#include <mutex>
#include "gpu_android.h"

#define LOG_TAG "PCSX"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" {
    #include "psxcommon.h"
    #include "r3000a.h"
    #include "cdrom.h"
    #include "misc.h"
    #include "gpu.h"
    #include "plugins.h"

    int  EmuInit(void);
    void EmuShutdown(void);
    int  LoadCdrom(void);
    void SetIsoFile(const char* filename);
    void psxReset(void);

    // Function that wraps the core execution loop
    void psxExecute(void) {
        while (!psxRegs.stop) {
            psxCpu->Execute(&psxRegs);
        }
    }

    // Frontend stubs normally provided by libretro.c or similar
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

    const char *SysLibError() { return ""; }
    void* SysLoadLibrary(const char* name) { return nullptr; }
    void* SysLoadSym(void* lib, const char* name) { return nullptr; }
    void  SysCloseLibrary(void* lib) {}
    void SysRunGui() {}
    void SysClose() {}

    static std::atomic<uint16_t> g_pad_state[2]{0, 0};

    void padReset() {}
    int  padFreeze(void *f, int mode) { return 0; }
    int  padToggleAnalog(unsigned int index) { return 0; }

    unsigned char PAD1_poll(unsigned char c, int *p) {
        // Simple digital pad polling: return low byte or high byte of mask depending on phase
        uint16_t mask = ~g_pad_state[0].load(); // PS1 uses active-low
        if (c == 0x42) return (mask & 0xFF);
        if (c == 0x43) return (mask >> 8);
        return 0xFF;
    }

    unsigned char PAD2_poll(unsigned char c, int *p) {
        uint16_t mask = ~g_pad_state[1].load();
        if (c == 0x42) return (mask & 0xFF);
        if (c == 0x43) return (mask >> 8);
        return 0xFF;
    }

    unsigned char PAD1_startPoll(void) { return 0; }
    unsigned char PAD2_startPoll(void) { return 0; }

    void pl_frame_limit() {}
    unsigned short in_keystate[8];

    void ndrc_freeze(void* f, int mode) {}
    void* ndrc_g = nullptr;

    uint16_t* psxVuw = nullptr;
}

static std::atomic<bool> g_emu_running{false};
static std::mutex g_emu_mutex;

extern "C" int PCSX_Run(const char* biosPath, const char* discPath, const char* saveDir) {
    std::lock_guard<std::mutex> lock(g_emu_mutex);
    if (g_emu_running.load()) {
        LOGE("PCSX_Run: already running");
        return -100;
    }

    if (!biosPath || !discPath || !saveDir) {
        LOGE("PCSX_Run: null path");
        return -1;
    }
    LOGI("PCSX_Run bios=%s disc=%s saves=%s", biosPath, discPath, saveDir);

    if (!psxVuw) {
        psxVuw = (uint16_t*)malloc(1024 * 512 * 2);
    }

    memset(&Config, 0, sizeof(Config));
    if (std::string(biosPath) == "HLE") {
        Config.HLE = TRUE;
        LOGI("PCSX_Run: Using HLE BIOS fallback");
    } else {
        strncpy(Config.Bios[0], biosPath, 63);
        Config.HLE = FALSE;
    }

    // Set up paths from provided saveDir
    strncpy(Config.BiosDir, biosPath, MAXPATHLEN - 1);
    char mcd1[MAXPATHLEN], mcd2[MAXPATHLEN];
    snprintf(mcd1, MAXPATHLEN, "%s/mcd1.mcr", saveDir);
    snprintf(mcd2, MAXPATHLEN, "%s/mcd2.mcr", saveDir);
    strncpy(Config.Mcd1, mcd1, MAXPATHLEN - 1);
    strncpy(Config.Mcd2, mcd2, MAXPATHLEN - 1);

    Config.Cpu = CPU_INTERPRETER;

    // Correct PCSX-ReARMed plugin function names (using the pointers in plugins.h)
    if (GPU_init() != 0) {
        LOGE("GPU_init failed");
        return -2;
    }

    // Hook the Android-specific blitter and status interceptor
    GPU_vBlank = (GPUvBlank)GPU_vBlank_android;
    GPU_writeStatus = (GPUwriteStatus)GPU_writeStatus_android;

    if (SPU_init() != 0) {
        LOGE("SPU_init failed — continuing without audio");
    }

    if (EmuInit() != 0) {
        LOGE("EmuInit failed");
        GPU_shutdown();
        SPU_shutdown();
        return -3;
    }

    // Register VRAM so gpu_android can blit frames
    gpu_android_set_vram(psxVuw);

    // Open the disc image
    SetIsoFile(discPath);
    if (LoadCdrom() != 0) {
        LOGE("LoadCdrom failed: %s", discPath);
        EmuShutdown();
        GPU_shutdown();
        SPU_shutdown();
        return -4;
    }

    psxReset();
    g_emu_running.store(true);
    psxRegs.stop = 0;
    LOGI("PCSX starting emulator thread");

    // Run on a dedicated thread so we never block the JNI caller
    std::thread emuThread([]() {
        psxExecute();
        LOGI("PCSX execution ended, cleaning up...");

        EmuShutdown();
        GPU_shutdown();
        SPU_shutdown();

        if (psxVuw) {
            free(psxVuw);
            psxVuw = nullptr;
        }

        g_emu_running.store(false);
    });
    emuThread.detach();

    return 0;   // return immediately — emu runs in background
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_NativeEmulatorBridge_stopGameNative(JNIEnv*, jobject) {
    LOGI("PCSX stopGameNative requested");
    psxRegs.stop = 1;
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_NativeEmulatorBridge_updateInputNative(JNIEnv*, jobject, jint padIndex, jint buttonMask) {
    if (padIndex >= 0 && padIndex < 2) {
        g_pad_state[padIndex].store((uint16_t)buttonMask);
    }
}
