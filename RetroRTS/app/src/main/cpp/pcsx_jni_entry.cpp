#include <jni.h>
#include <android/log.h>
#include <atomic>
#include <cstdint>

#define LOG_TAG "PCSX"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
std::atomic<bool> g_emu_running{false};
std::atomic<uint16_t> g_pad_state[2]{0, 0};
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_NativeEmulatorBridge_stopGameNative(JNIEnv*, jclass) {
    LOGI("PCSX stopGameNative requested");
    g_emu_running.store(false);
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_NativeEmulatorBridge_updateInputNative(JNIEnv*, jclass, jint padIndex, jint buttonMask) {
    if (padIndex >= 0 && padIndex < 2) {
        g_pad_state[padIndex].store(static_cast<uint16_t>(buttonMask));
    }
}
