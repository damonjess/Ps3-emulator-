#include <jni.h>
#include <atomic>
#include <string>
#include <android/log.h>
#include <dlfcn.h>
#include <fstream>
#include "amiga_core.h"
#include "libretro_bridge.h"

#define LOG_TAG "RetroRTS_Amiga"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
std::atomic<bool> g_amiga_running{false};
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

    LOGI("Starting Amiga via bridge: game=%s config=%s", gpath, cpath);

    int init_result = retrorts::uae_init(cpath);
    if (init_result != 0) {
        LOGE("UAE initialization failed via bridge with code %d", init_result);
        env->ReleaseStringUTFChars(gamePath, gpath);
        env->ReleaseStringUTFChars(configPath, cpath);
        return JNI_FALSE;
    }

    g_amiga_running.store(true);
    LOGI("Amiga emulator started successfully via bridge");

    env->ReleaseStringUTFChars(gamePath, gpath);
    env->ReleaseStringUTFChars(configPath, cpath);
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_AmigaBridge_stopAmigaNative(JNIEnv*, jobject) {
    if (!g_amiga_running.load()) return;

    retrorts::LibretroHost::getInstance().stop();

    g_amiga_running.store(false);
    LOGI("Amiga emulator stopped via bridge");
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_AmigaBridge_updateInputNative(
    JNIEnv*, jobject, jint port, jint buttonMask) {
    // Input handling via bridge will be added to LibretroHost
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_AmigaBridge_setSurfaceNative(
    JNIEnv*, jobject, jobject surface) {
    // Surface handling via bridge will be added to LibretroHost
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_retrorts_ui_AmigaBridge_isRunningNative(JNIEnv*, jobject) {
    return g_amiga_running.load() ? JNI_TRUE : JNI_FALSE;
}
