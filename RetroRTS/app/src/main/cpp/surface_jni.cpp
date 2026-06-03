#include <jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <dlfcn.h>

#define LOG_TAG "RetroRTS_Surface"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_NativeEmulatorBridge_setSurfaceNative(
JNIEnv* env, jobject, jobject surface) {
    ANativeWindow* window = surface
        ? ANativeWindow_fromSurface(env, surface)
        : nullptr;

    // Pass to DOSBox-Pure via its published symbol dosbox_pure_set_window
    void* lib = dlopen("libdosbox_pure.so", RTLD_NOW | RTLD_NOLOAD);
    if (lib) {
        typedef void (*set_window_t)(ANativeWindow*);
        auto fn = reinterpret_cast<set_window_t>(dlsym(lib, "dosbox_pure_set_window"));
        if (fn) {
            fn(window);
            LOGI("dosbox_pure_set_window called with %s",
                 window ? "valid surface" : "null (surface destroyed)");
        } else {
            LOGE("dosbox_pure_set_window symbol not found");
        }
    } else {
        LOGE("libdosbox_pure.so not available");
    }

    if (window) ANativeWindow_release(window);
}
