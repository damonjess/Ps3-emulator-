#include <jni.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>
#include <android/log.h>
#include <dlfcn.h>

#define LOG_TAG "RetroRTS_Surface"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

#include "gpu_android.h"

extern "C" JNIEXPORT void JNICALL
Java_com_retrorts_ui_NativeEmulatorBridge_setSurfaceNative(
JNIEnv* env, jobject, jobject surface) {
    ANativeWindow* window = surface
        ? ANativeWindow_fromSurface(env, surface)
        : nullptr;

    gpu_android_set_window(window);

    if (window) ANativeWindow_release(window);
}
