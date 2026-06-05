#include "amiga_core.h"
#include <fstream>
#include <android/log.h>

#define LOG_TAG "RetroRTS_Amiga"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)

namespace retrorts::amiga {

constexpr const char* kBiosDir = "/sdcard/RetroRTS/system/amiga";

bool fileExists(const std::string& p) {
    return std::ifstream(p, std::ios::binary).good();
}

AmigaLaunchResult LaunchAmigaGame(const std::string& romPath) {
    if (romPath.empty())
        return {false, "Amiga launch failed: empty ROM path", ""};

    // Check for common Kickstart versions
    std::string bestBios = "";
    if (fileExists(std::string(kBiosDir) + "/kick13.rom")) {
        bestBios = std::string(kBiosDir) + "/kick13.rom";
    } else if (fileExists(std::string(kBiosDir) + "/kick31.rom")) {
        bestBios = std::string(kBiosDir) + "/kick31.rom";
    }

    if (bestBios.empty()) {
        return {false, "Amiga launch failed: No Kickstart ROM found in /sdcard/RetroRTS/system/amiga/", "", ""};
    }

    LOGI("Amiga ready: rom=%s bios=%s", romPath.c_str(), bestBios.c_str());
    return {true, "Amiga core ready", romPath, bestBios};
}

}  // namespace retrorts::amiga
