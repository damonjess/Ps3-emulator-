#include <jni.h>
#include <atomic>
#include <string>
#include <android/log.h>
#include <dlfcn.h>
#include <fstream>
#include "amiga_core.h"

#define LOG_TAG "RetroRTS_Amiga"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
std::atomic<bool> g_amiga_running{false};
void* g_uae_lib{nullptr};

// UAE function signatures
typedef int (*uae_init_t)(const char* config_path);
typedef void (*uae_run_t)(void);
typedef void (*uae_stop_t)(void);
typedef void (*uae_input_t)(int port, int button_mask);
typedef void (*uae_set_surface_t)(void* surface);
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_retrorts_ui_AmigaBridge_startAmigaNative(
    JNIEnv* env, jobject, jstring gamePath, jstring configPath) {

    if (g_amiga_running.load()) return JNI_TRUE;
    if (!gamePath || !configPath) return JNI_FALSE;

    const char* gpath = env->GetStringUTFChars(gamePath, nullptr);
    const char* cpath = env->GetStringUTFChars(configPath, nullptr);

    if (!gpath || !cpath) {
        if (gpath) env->ReleaseStringUTFChars(gamePath, gpath);
        if (cpath) env->ReleaseStringUTFChars(configPath, cpath);
        return JNI_FALSE;
    }

    LOGI("Starting Amiga: game=%s config=%s", gpath, cpath);

    // Try to load UAE library (libpuae.so or similar)
    void* lib = dlopen("libpuae.so", RTLD_NOW);
    if (!lib) {
        lib = dlopen("libuae.so", RTLD_NOW);
    }
    if (!lib) {
        LOGE("UAE library not found: %s", dlerror());
        env->ReleaseStringUTFChars(gamePath, gpath);
        env->ReleaseStringUTFChars(configPath, cpath);
        return JNI_FALSE;
    }

    // Get UAE initialization function
    auto init_fn = reinterpret_cast<uae_init_t>(dlsym(lib, "uae_init"));
    if (!init_fn) {
        LOGE("uae_init symbol not found");
        dlclose(lib);
        env->ReleaseStringUTFChars(gamePath, gpath);
        env->ReleaseStringUTFChars(configPath, cpath);
        return JNI_FALSE;
    }

    // Initialize UAE with config
    int init_result = init_fn(cpath);
    if (init_result != 0) {
        LOGE("UAE initialization failed with code %d", init_result);
        dlclose(lib);
        env->ReleaseStringUTFChars(gamePath, gpath);
        env->ReleaseStringUTFChars(configPath, cpath);
        return JNI_FALSE;
    }

    g_uae_lib = lib;
    g_amiga_running.store(true);

    LOGI("Amiga emulator started successfully");

    env->ReleaseStringUTFChars(gamePath, gpath);
    env->ReleaseStringUTFChars(configPath, cpath);
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_AmigaBridge_stopAmigaNative(JNIEnv*, jobject) {
    if (!g_amiga_running.load()) return;

    if (g_uae_lib) {
        auto stop_fn = reinterpret_cast<uae_stop_t>(dlsym(g_uae_lib, "uae_stop"));
        if (stop_fn) {
            stop_fn();
        }
        dlclose(g_uae_lib);
        g_uae_lib = nullptr;
    }

    g_amiga_running.store(false);
    LOGI("Amiga emulator stopped");
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_AmigaBridge_updateInputNative(
    JNIEnv*, jobject, jint port, jint buttonMask) {

    if (!g_amiga_running.load() || !g_uae_lib) return;

    auto input_fn = reinterpret_cast<uae_input_t>(dlsym(g_uae_lib, "uae_input"));
    if (input_fn) {
        input_fn(port, buttonMask);
    }
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_AmigaBridge_setSurfaceNative(
    JNIEnv*, jobject, jobject surface) {

    if (!g_amiga_running.load() || !g_uae_lib) return;

    auto set_surface_fn = reinterpret_cast<uae_set_surface_t>(dlsym(g_uae_lib, "uae_set_surface"));
    if (set_surface_fn) {
        set_surface_fn(surface);
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_retrorts_ui_AmigaBridge_isRunningNative(JNIEnv*, jobject) {
    return g_amiga_running.load() ? JNI_TRUE : JNI_FALSE;
}
