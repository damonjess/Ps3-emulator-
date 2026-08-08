#include <jni.h>
#include <android/log.h>
#include <atomic>
#include <cstdint>
#include "libretro_bridge.h"

#define LOG_TAG "PCSX"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
std::atomic<bool> g_emu_running{false};
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_NativeEmulatorBridge_stopGameNative(JNIEnv*, jclass) {
    LOGI("NativeEmulatorBridge stopGameNative requested");
    g_emu_running.store(false);
    retrorts::LibretroHost::getInstance().stop();
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_NativeEmulatorBridge_updateInputNative(JNIEnv*, jclass, jint padIndex, jint buttonMask) {
    retrorts::LibretroHost::getInstance().updateJoypad(padIndex, static_cast<uint16_t>(buttonMask));
}
