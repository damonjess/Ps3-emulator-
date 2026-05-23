#include <jni.h>
#include <atomic>
#include <string>
#include <fstream>
#include <algorithm>
#include "dosbox_audio.h"

namespace {
std::atomic<bool> g_running{false};
DosboxAudioEngine g_audio;
std::atomic<int> g_cpu_cycles{35000};     // tuned baseline for Win98-era RTS on modern ARM64
std::atomic<int> g_frame_cap{60};         // cap to 60fps
std::atomic<int> g_thermal_level{0};      // 0=nominal, 1=warm, 2=hot, 3=critical
std::atomic<float> g_mock_fps{60.0f};
std::atomic<float> g_mock_cpu{42.0f};

int effective_cycles() {
    int base = g_cpu_cycles.load();
    int thermal = g_thermal_level.load();
    if (thermal >= 3) return std::max(8000, base / 3);
    if (thermal == 2) return std::max(12000, base / 2);
    if (thermal == 1) return std::max(18000, (base * 3) / 4);
    return base;
}
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_retrorts_ui_DosboxBridge_startDosboxNative(JNIEnv* env, jobject, jstring gameDir, jstring configPath) {
    if (g_running.load()) return JNI_TRUE;

    if (gameDir == nullptr || configPath == nullptr) return JNI_FALSE;
    const char* game_dir_c = env->GetStringUTFChars(gameDir, nullptr);
    const char* config_path_c = env->GetStringUTFChars(configPath, nullptr);
    if (game_dir_c == nullptr || config_path_c == nullptr) return JNI_FALSE;
    (void)game_dir_c;
    (void)config_path_c;
    env->ReleaseStringUTFChars(gameDir, game_dir_c);
    env->ReleaseStringUTFChars(configPath, config_path_c);

    g_audio.start(48000, 2);
    g_running.store(true);
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL Java_com_retrorts_ui_DosboxBridge_stopDosboxNative(JNIEnv*, jobject) {
    if (!g_running.load()) return;
    g_audio.stop();
    g_running.store(false);
}

extern "C" JNIEXPORT void JNICALL Java_com_retrorts_ui_DosboxBridge_submitAudioPcm16Native(JNIEnv* env, jobject, jshortArray buffer, jint frames, jint channels) {
    if (!g_running.load() || buffer == nullptr) return;
    jshort* ptr = env->GetShortArrayElements(buffer, nullptr);
    g_audio.submitPcm16(reinterpret_cast<int16_t*>(ptr), frames, channels);
    env->ReleaseShortArrayElements(buffer, ptr, JNI_ABORT);
}

extern "C" JNIEXPORT void JNICALL Java_com_retrorts_ui_DosboxBridge_setVolumeNative(JNIEnv*, jobject, jfloat volume) { g_audio.setVolume(volume); }

extern "C" JNIEXPORT jboolean JNICALL Java_com_retrorts_ui_DosboxBridge_saveStateNative(JNIEnv* env, jobject, jstring gameId, jint slot, jstring path) {
    if (gameId == nullptr || path == nullptr) return JNI_FALSE;
    const char* game_id = env->GetStringUTFChars(gameId, nullptr);
    if (path == nullptr) return JNI_FALSE;
    const char* save_path = env->GetStringUTFChars(path, nullptr);
    if (save_path == nullptr) return JNI_FALSE;
    if (game_id == nullptr || save_path == nullptr) return JNI_FALSE;
    std::ofstream out(save_path, std::ios::binary);
    bool ok = false;
    if (out.good()) { out << "GAME=" << game_id << "\nSLOT=" << slot << "\nSTATE=PLACEHOLDER\n"; ok = true; }
    out.close();
    env->ReleaseStringUTFChars(gameId, game_id);
    env->ReleaseStringUTFChars(path, save_path);
    return ok ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_retrorts_ui_DosboxBridge_loadStateNative(JNIEnv* env, jobject, jstring, jint, jstring path) {
    if (path == nullptr) return JNI_FALSE;
    const char* save_path = env->GetStringUTFChars(path, nullptr);
    if (save_path == nullptr) return JNI_FALSE;
    std::ifstream in(save_path, std::ios::binary);
    bool ok = in.good();
    in.close();
    env->ReleaseStringUTFChars(path, save_path);
    return ok ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT void JNICALL Java_com_retrorts_ui_DosboxBridge_setCpuCyclesNative(JNIEnv*, jobject, jint cycles) {
    g_cpu_cycles.store(std::clamp(static_cast<int>(cycles), 5000, 200000));
}

extern "C" JNIEXPORT void JNICALL Java_com_retrorts_ui_DosboxBridge_setFrameCapNative(JNIEnv*, jobject, jint fps) {
    g_frame_cap.store(std::clamp(static_cast<int>(fps), 30, 120));
    g_mock_fps.store(static_cast<float>(g_frame_cap.load()));
}

extern "C" JNIEXPORT void JNICALL Java_com_retrorts_ui_DosboxBridge_notifyThermalLevelNative(JNIEnv*, jobject, jint level) {
    g_thermal_level.store(std::clamp(static_cast<int>(level), 0, 3));
    const int eff = effective_cycles();
    const float ratio = static_cast<float>(eff) / static_cast<float>(std::max(1, g_cpu_cycles.load()));
    g_mock_cpu.store(35.0f * ratio + 20.0f);
}

extern "C" JNIEXPORT jfloatArray JNICALL Java_com_retrorts_ui_DosboxBridge_getPerfStatsNative(JNIEnv* env, jobject) {
    jfloatArray arr = env->NewFloatArray(2);
    float values[2] = {g_mock_fps.load(), g_mock_cpu.load()};
    env->SetFloatArrayRegion(arr, 0, 2, values);
    return arr;
}
