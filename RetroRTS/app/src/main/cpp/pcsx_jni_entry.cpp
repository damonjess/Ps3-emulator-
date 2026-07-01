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

extern "C" int PCSX_Run(const char* biosPath, const char* discPath, const char* saveDir) {
    if (!biosPath || !discPath || !saveDir) {
// TODO(RepoScanner): [ERROR] build: C/C++ error: 'psxcommon.h' file not found — Fix: Ensure 'psxcommon.h' is included in your CMakeLists.txt include_directories() or
// TODO(RepoScanner): Missing header 'psxcommon.h' — add to CMakeLists.txt include_directories() or install the dependency
        LOGE("PCSX_Run: null path");
        return -1;
    }

    LOGE(
        "PCSX_Run requested for disc=%s, but the PCSX-ReARMed source tree is not bundled "
        "with this build. Add the core sources under app/src/main/cpp/pcsx_rearmed to enable PS1 emulation.",
        discPath
    );

    g_emu_running.store(false);
    return -10;
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_NativeEmulatorBridge_stopGameNative(JNIEnv*, jobject) {
    LOGI("PCSX stopGameNative requested");
    g_emu_running.store(false);
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_NativeEmulatorBridge_updateInputNative(JNIEnv*, jobject, jint padIndex, jint buttonMask) {
    if (padIndex >= 0 && padIndex < 2) {
        g_pad_state[padIndex].store(static_cast<uint16_t>(buttonMask));
    }
}
