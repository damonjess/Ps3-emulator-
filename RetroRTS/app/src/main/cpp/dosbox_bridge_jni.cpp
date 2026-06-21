#include <jni.h>
#include <atomic>
#include <string>
#include <android/log.h>
#include <dlfcn.h>
#include <fstream>
#include <algorithm>
#include "dosbox_audio.h"

#define LOG_TAG "RetroRTS"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
std::atomic<bool> g_running{false};
DosboxAudioEngine g_audio;
std::atomic<int>   g_cpu_cycles{35000};
std::atomic<int>   g_frame_cap{120};
std::atomic<int>   g_thermal_level{0};
std::atomic<float> g_fps{120.0f};
std::atomic<float> g_cpu_pct{42.0f};
void* g_dosbox_lib{nullptr};

int effective_cycles() {
    int base = g_cpu_cycles.load();
    switch (g_thermal_level.load()) {
        case 3: return std::max(8000,  base / 3);
        case 2: return std::max(12000, base / 2);
        case 1: return std::max(18000, (base * 3) / 4);
        default: return base;
    }
}
}

// ── start ─────────────────────────────────────────────────────────────────
extern "C" JNIEXPORT jboolean JNICALL
Java_com_retrorts_ui_DosboxBridge_startDosboxNative(
JNIEnv* env, jobject, jstring gameDir, jstring configPath) {
if (g_running.load()) return JNI_TRUE;
if (!gameDir || !configPath) return JNI_FALSE;

const char* dir  = env->GetStringUTFChars(gameDir,    nullptr);
const char* conf = env->GetStringUTFChars(configPath, nullptr);
if (!dir || !conf) {
    if (dir) env->ReleaseStringUTFChars(gameDir, dir);
    if (conf) env->ReleaseStringUTFChars(configPath, conf);
    return JNI_FALSE;
}

LOGI("startDosbox dir=%s conf=%s", dir, conf);

// Tune cycles per game
std::string d(dir);
if (d.find("Dune2000") != std::string::npos || d.find("dune") != std::string::npos)
    g_cpu_cycles.store(35000);
else if (d.find("RedAlert") != std::string::npos || d.find("cnc") != std::string::npos)
    g_cpu_cycles.store(30000);

// DOSBox-Pure exposes dosbox_run(conf_path) via its .so loaded by the AAR.
// Do not report success until the native core is present and accepts the launch.
void* lib = dlopen("libdosbox_pure.so", RTLD_NOW);
if (!lib) {
    LOGE("libdosbox_pure.so not available: %s", dlerror());
    env->ReleaseStringUTFChars(gameDir,    dir);
    env->ReleaseStringUTFChars(configPath, conf);
    return JNI_FALSE;
}

typedef int (*dosbox_run_t)(const char*);
auto fn = reinterpret_cast<dosbox_run_t>(dlsym(lib, "dosbox_pure_run"));
if (!fn) {
    LOGE("dosbox_pure_run symbol not found in libdosbox_pure.so: %s", dlerror());
    dlclose(lib);
    env->ReleaseStringUTFChars(gameDir,    dir);
    env->ReleaseStringUTFChars(configPath, conf);
    return JNI_FALSE;
}

const int runResult = fn(conf);
if (runResult != 0) {
    LOGE("dosbox_pure_run failed with code %d", runResult);
    dlclose(lib);
    env->ReleaseStringUTFChars(gameDir,    dir);
    env->ReleaseStringUTFChars(configPath, conf);
    return JNI_FALSE;
}

g_dosbox_lib = lib;
g_audio.start(48000, 2);
g_running.store(true);

env->ReleaseStringUTFChars(gameDir,    dir);
env->ReleaseStringUTFChars(configPath, conf);
return JNI_TRUE;
}

// ── stop ──────────────────────────────────────────────────────────────────
extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_DosboxBridge_stopDosboxNative(JNIEnv*, jobject) {
if (!g_running.load()) return;
g_audio.stop();
g_running.store(false);
if (g_dosbox_lib) {
    dlclose(g_dosbox_lib);
    g_dosbox_lib = nullptr;
}
}

// ── audio PCM submit ──────────────────────────────────────────────────────
extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_DosboxBridge_submitAudioPcm16Native(
JNIEnv* env, jobject, jshortArray buffer, jint frames, jint channels) {
if (!g_running.load() || !buffer) return;
jshort* ptr = env->GetShortArrayElements(buffer, nullptr);
g_audio.submitPcm16(reinterpret_cast<int16_t*>(ptr), frames, channels);
env->ReleaseShortArrayElements(buffer, ptr, JNI_ABORT);
}

// ── controls ──────────────────────────────────────────────────────────────
extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_DosboxBridge_setVolumeNative(JNIEnv*, jobject, jfloat v)
{ g_audio.setVolume(v); }

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_DosboxBridge_setCpuCyclesNative(JNIEnv*, jobject, jint c)
{ g_cpu_cycles.store(std::clamp((int)c, 5000, 200000)); }

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_DosboxBridge_setFrameCapNative(JNIEnv*, jobject, jint fps) {
g_frame_cap.store(std::clamp((int)fps, 30, 120));
g_fps.store((float)g_frame_cap.load());
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_DosboxBridge_notifyThermalLevelNative(JNIEnv*, jobject, jint lvl) {
g_thermal_level.store(std::clamp((int)lvl, 0, 3));
float ratio = (float)effective_cycles() / (float)std::max(1, g_cpu_cycles.load());
g_cpu_pct.store(35.0f * ratio + 20.0f);
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_retrorts_ui_DosboxBridge_getPerfStatsNative(JNIEnv* env, jobject) {
jfloatArray arr = env->NewFloatArray(2);
float v[2] = { g_fps.load(), g_cpu_pct.load() };
env->SetFloatArrayRegion(arr, 0, 2, v);
return arr;
}

// ── save / load state ─────────────────────────────────────────────────────
extern "C" JNIEXPORT jboolean JNICALL
Java_com_retrorts_ui_DosboxBridge_saveStateNative(
JNIEnv* env, jobject, jstring gameId, jint slot, jstring path) {
if (!gameId || !path) return JNI_FALSE;
const char* gid  = env->GetStringUTFChars(gameId, nullptr);
const char* fpath = env->GetStringUTFChars(path,   nullptr);
bool ok = false;
if (gid && fpath) {
std::ofstream out(fpath, std::ios::binary);
if (out.good()) { out << "GAME=" << gid << "\nSLOT=" << slot << "\nSTATE=PLACEHOLDER\n"; ok = true; }
}
env->ReleaseStringUTFChars(gameId, gid);
env->ReleaseStringUTFChars(path,   fpath);
return ok ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_retrorts_ui_DosboxBridge_loadStateNative(
JNIEnv* env, jobject, jstring, jint, jstring path) {
if (!path) return JNI_FALSE;
const char* fpath = env->GetStringUTFChars(path, nullptr);
bool ok = std::ifstream(fpath, std::ios::binary).good();
env->ReleaseStringUTFChars(path, fpath);
return ok ? JNI_TRUE : JNI_FALSE;
}
