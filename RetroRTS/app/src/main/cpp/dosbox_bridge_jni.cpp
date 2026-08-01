#include <jni.h>
#include <android/log.h>
#include <dlfcn.h>
#include <string>
#include <atomic>

#define LOG_TAG "RetroRTS_DOSBox"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace {
    std::atomic<bool> g_dosbox_running{false};
    void* g_dosbox_lib{nullptr};

    typedef int  (*db_init_t)(const char* configPath, const char* saveDir);
    typedef void (*db_shutdown_t)(void);
    typedef void (*db_set_cycles_t)(int cycles);
    typedef void (*db_set_framerate_t)(int fps);
    typedef void (*db_set_volume_t)(float volume);
    typedef void (*db_thermal_t)(int level);
    typedef void (*db_get_stats_t)(float* out);
    typedef int  (*db_savestate_t)(const char* gameId, int slot, const char* path);
    typedef int  (*db_loadstate_t)(const char* gameId, int slot, const char* path);

    db_init_t        fn_init       = nullptr;
    db_shutdown_t    fn_shutdown   = nullptr;
    db_set_cycles_t  fn_set_cycles = nullptr;
    db_set_framerate_t fn_set_fps  = nullptr;
    db_set_volume_t  fn_set_volume = nullptr;
    db_thermal_t     fn_thermal    = nullptr;
    db_get_stats_t   fn_stats      = nullptr;
    db_savestate_t   fn_save       = nullptr;
    db_loadstate_t   fn_load       = nullptr;

    bool loadSymbols() {
        if (!g_dosbox_lib) return false;
        fn_init       = reinterpret_cast<db_init_t>(dlsym(g_dosbox_lib, "dosbox_init"));
        if (!fn_init) fn_init = reinterpret_cast<db_init_t>(dlsym(g_dosbox_lib, "DOSBOX_Init"));
        fn_shutdown   = reinterpret_cast<db_shutdown_t>(dlsym(g_dosbox_lib, "dosbox_shutdown"));
        fn_set_cycles = reinterpret_cast<db_set_cycles_t>(dlsym(g_dosbox_lib, "dosbox_set_cycles"));
        fn_set_fps    = reinterpret_cast<db_set_framerate_t>(dlsym(g_dosbox_lib, "dosbox_set_framerate"));
        fn_set_volume = reinterpret_cast<db_set_volume_t>(dlsym(g_dosbox_lib, "dosbox_set_volume"));
        fn_thermal    = reinterpret_cast<db_thermal_t>(dlsym(g_dosbox_lib, "dosbox_notify_thermal"));
        fn_stats      = reinterpret_cast<db_get_stats_t>(dlsym(g_dosbox_lib, "dosbox_get_perf_stats"));
        fn_save       = reinterpret_cast<db_savestate_t>(dlsym(g_dosbox_lib, "dosbox_save_state"));
        fn_load       = reinterpret_cast<db_loadstate_t>(dlsym(g_dosbox_lib, "dosbox_load_state"));
        return fn_init != nullptr;
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_retrorts_ui_DosboxBridge_startDosboxNative(
    JNIEnv* env, jclass, jstring gameDir, jstring configPath) {

    if (g_dosbox_running.load()) return JNI_TRUE;

    const char* gdir = env->GetStringUTFChars(gameDir, nullptr);
    const char* cpath = env->GetStringUTFChars(configPath, nullptr);
    if (!gdir || !cpath) {
        if (gdir) env->ReleaseStringUTFChars(gameDir, gdir);
        if (cpath) env->ReleaseStringUTFChars(configPath, cpath);
        return JNI_FALSE;
    }

    g_dosbox_lib = dlopen("libdosbox_pure.so", RTLD_NOW);
    if (!g_dosbox_lib) g_dosbox_lib = dlopen("libdosbox.so", RTLD_NOW);
    if (!g_dosbox_lib) {
        LOGE("Failed to load DOSBox library: %s", dlerror());
        env->ReleaseStringUTFChars(gameDir, gdir);
        env->ReleaseStringUTFChars(configPath, cpath);
        return JNI_FALSE;
    }

    if (!loadSymbols()) {
        LOGE("DOSBox symbols not found");
        dlclose(g_dosbox_lib);
        g_dosbox_lib = nullptr;
        env->ReleaseStringUTFChars(gameDir, gdir);
        env->ReleaseStringUTFChars(configPath, cpath);
        return JNI_FALSE;
    }

    int r = fn_init(cpath, gdir);
    env->ReleaseStringUTFChars(gameDir, gdir);
    env->ReleaseStringUTFChars(configPath, cpath);

    if (r != 0) {
        LOGE("DOSBox init failed: %d", r);
        dlclose(g_dosbox_lib);
        g_dosbox_lib = nullptr;
        return JNI_FALSE;
    }

    g_dosbox_running.store(true);
    LOGI("DOSBox started successfully");
    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_DosboxBridge_stopDosboxNative(JNIEnv*, jclass) {
    if (!g_dosbox_running.load()) return;
    if (fn_shutdown) fn_shutdown();
    if (g_dosbox_lib) {
        dlclose(g_dosbox_lib);
        g_dosbox_lib = nullptr;
    }
    g_dosbox_running.store(false);
    LOGI("DOSBox stopped");
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_DosboxBridge_setCpuCyclesNative(JNIEnv*, jclass, jint cycles) {
    if (fn_set_cycles) fn_set_cycles(cycles);
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_DosboxBridge_setFrameCapNative(JNIEnv*, jclass, jint fps) {
    if (fn_set_fps) fn_set_fps(fps);
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_DosboxBridge_setVolumeNative(JNIEnv*, jclass, jfloat volume) {
    if (fn_set_volume) fn_set_volume(volume);
}

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_DosboxBridge_notifyThermalLevelNative(JNIEnv*, jclass, jint level) {
    if (fn_thermal) fn_thermal(level);
}

extern "C" JNIEXPORT jfloatArray JNICALL
Java_com_retrorts_ui_DosboxBridge_getPerfStatsNative(JNIEnv* env, jclass) {
    float stats[2] = {0.0f, 0.0f};
    if (fn_stats) fn_stats(stats);
    jfloatArray arr = env->NewFloatArray(2);
    env->SetFloatArrayRegion(arr, 0, 2, stats);
    return arr;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_retrorts_ui_DosboxBridge_saveStateNative(
    JNIEnv* env, jclass, jstring gameId, jint slot, jstring path) {
    if (!fn_save) return JNI_FALSE;
    const char* gid = env->GetStringUTFChars(gameId, nullptr);
    const char* p   = env->GetStringUTFChars(path, nullptr);
    int r = fn_save(gid, slot, p);
    env->ReleaseStringUTFChars(gameId, gid);
    env->ReleaseStringUTFChars(path, p);
    return r == 0 ? JNI_TRUE : JNI_FALSE;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_retrorts_ui_DosboxBridge_loadStateNative(
    JNIEnv* env, jclass, jstring gameId, jint slot, jstring path) {
    if (!fn_load) return JNI_FALSE;
    const char* gid = env->GetStringUTFChars(gameId, nullptr);
    const char* p   = env->GetStringUTFChars(path, nullptr);
    int r = fn_load(gid, slot, p);
    env->ReleaseStringUTFChars(gameId, gid);
    env->ReleaseStringUTFChars(path, p);
    return r == 0 ? JNI_TRUE : JNI_FALSE;
}
