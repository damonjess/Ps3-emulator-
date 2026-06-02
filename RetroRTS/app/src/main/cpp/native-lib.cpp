#include <jni.h>
#include <string>
#include "emulator_core.h"

extern "C"
JNIEXPORT jstring JNICALL
Java_com_retrorts_ui_NativeEmulatorBridge_launchGame(JNIEnv* env, jobject, jstring console, jstring romPath) {
    if (console == nullptr || romPath == nullptr) {
        return env->NewStringUTF("");
    }

    const char* cConsole = env->GetStringUTFChars(console, nullptr);
    const char* cRomPath = env->GetStringUTFChars(romPath, nullptr);
    if (cConsole == nullptr || cRomPath == nullptr) {
        if (cConsole != nullptr) env->ReleaseStringUTFChars(console, cConsole);
        if (cRomPath != nullptr) env->ReleaseStringUTFChars(romPath, cRomPath);
        return env->NewStringUTF("");
    }

    std::string result = retrorts::LaunchGame(cConsole, cRomPath);

    env->ReleaseStringUTFChars(console, cConsole);
    env->ReleaseStringUTFChars(romPath, cRomPath);

    return env->NewStringUTF(result.c_str());
}
