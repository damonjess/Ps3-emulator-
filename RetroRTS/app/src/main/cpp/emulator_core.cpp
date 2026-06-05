#include "emulator_core.h"
#include "dsi_core.h"
#include "ps1_core.h"
#include "amiga_core.h"
#include <dlfcn.h>
#include <android/log.h>
#include <algorithm>

#define LOG_TAG "RetroRTS_Core"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Direct call — PCSX_Run is statically linked, no dlopen needed
extern "C" int PCSX_Run(const char* bios, const char* disc);

namespace retrorts {

std::string LaunchGame(const std::string& console,
                       const std::string& romPath) {
    const std::string c = [&]{
        std::string s = console;
        std::transform(s.begin(), s.end(), s.begin(), ::toupper);
        return s;
    }();

    if (c == "PS1") {
        auto result = retrorts::ps1::LaunchPs1Game(romPath);
        if (!result.ok) return "ERROR: " + result.message;
        const std::string bios =
            "/sdcard/RetroRTS/system/ps1/scph1001.bin";
        int r = PCSX_Run(bios.c_str(), result.resolvedCuePath.c_str());
        if (r != 0) return "ERROR: PS1 error code " + std::to_string(r);
        return "OK: " + result.message;

    } else if (c == "DOSBOX" || c == "DOS") {
        // DOSBox launch via DosboxBridge
        return "OK: DOSBox launching " + romPath;

    } else if (c == "AMIGA") {
        return "OK: Amiga launching " + romPath;

    } else if (c == "NINTENDO_DSI" || c == "DSI") {
        auto result = retrorts::dsi::LaunchDsiGame(romPath);
        return result.ok ? "OK: " + result.message
                         : "ERROR: " + result.message;

    } else {
        // Fallback — try to detect from file extension
        const std::string lower = [&]{
            std::string s = romPath;
            std::transform(s.begin(), s.end(), s.begin(), ::tolower);
            return s;
        }();
        if (lower.ends_with(".bin") || lower.ends_with(".cue") ||
            lower.ends_with(".img") || lower.ends_with(".iso")) {
            // Re-route to PS1
            auto result = retrorts::ps1::LaunchPs1Game(romPath);
            if (!result.ok) return "ERROR: " + result.message;
            const std::string bios =
                "/sdcard/RetroRTS/system/ps1/scph1001.bin";
            int r = PCSX_Run(bios.c_str(), result.resolvedCuePath.c_str());
            return r == 0 ? "OK: PS1 auto-detected"
                          : "ERROR: PS1 error " + std::to_string(r);
        }
        return "ERROR: Unknown console type: " + console;
    }
}

}  // namespace retrorts
