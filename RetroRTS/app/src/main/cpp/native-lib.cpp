#include <jni.h>
#include <string>
#include "emulator_core.h"

extern "C"
JNIEXPORT jstring JNICALL
Java_com_retrorts_ui_NativeEmulatorBridge_launchGameNative(JNIEnv* env, jobject, jstring console, jstring romPath, jstring cacheDir, jstring saveDir) {
    if (console == nullptr || romPath == nullptr || cacheDir == nullptr || saveDir == nullptr) {
        return env->NewStringUTF("");
    }

    const char* cConsole  = env->GetStringUTFChars(console,  nullptr);
    const char* cRomPath  = env->GetStringUTFChars(romPath,  nullptr);
    const char* cCacheDir = env->GetStringUTFChars(cacheDir, nullptr);
    const char* cSaveDir  = env->GetStringUTFChars(saveDir,  nullptr);

    if (!cConsole || !cRomPath || !cCacheDir || !cSaveDir) {
        if (cConsole)  env->ReleaseStringUTFChars(console,  cConsole);
        if (cRomPath)  env->ReleaseStringUTFChars(romPath,  cRomPath);
        if (cCacheDir) env->ReleaseStringUTFChars(cacheDir, cCacheDir);
        if (cSaveDir)  env->ReleaseStringUTFChars(saveDir,  cSaveDir);
        return env->NewStringUTF("");
    }

    std::string result = retrorts::LaunchGame(cConsole, cRomPath, cCacheDir, cSaveDir);

    env->ReleaseStringUTFChars(console,  cConsole);
    env->ReleaseStringUTFChars(romPath,  cRomPath);
    env->ReleaseStringUTFChars(cacheDir, cCacheDir);
    env->ReleaseStringUTFChars(saveDir,  cSaveDir);

    return env->NewStringUTF(result.c_str());
}
