#include "amiga_core.h"
#include <fstream>
#include <android/log.h>
#include <dlfcn.h>
#include <algorithm>

#define LOG_TAG "RetroRTS_Amiga"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace retrorts::amiga {

constexpr const char* kBiosDir = "/sdcard/RetroRTS/system/amiga";
constexpr const char* kGamesDir = "/sdcard/RetroRTS/Games/Amiga";

bool fileExists(const std::string& p) {
    return std::ifstream(p, std::ios::binary).good();
}

long fileSize(const std::string& p) {
    std::ifstream f(p, std::ios::ate | std::ios::binary);
    return f.good() ? static_cast<long>(f.tellg()) : -1L;
}

std::string toLower(std::string s) {
    std::transform(s.begin(), s.end(), s.begin(),
        [](unsigned char c){ return static_cast<char>(std::tolower(c)); });
    return s;
}

std::string ext(const std::string& path) {
    auto pos = path.rfind('.');
    return pos == std::string::npos ? "" : toLower(path.substr(pos));
}

// Detect which Kickstart ROM version to use based on game requirements
std::string selectKickstartRom(const std::string& gamePath) {
    // Dune II works best with Kickstart 1.3 or 3.1
    // Try in order of preference
    const char* kickstarts[] = {
        "kick13.rom",  // Kickstart 1.3 (most compatible for classic games)
        "kick31.rom",  // Kickstart 3.1 (AGA support)
        "kick12.rom",  // Kickstart 1.2
        "kick20.rom",  // Kickstart 2.0
        "kick30.rom",  // Kickstart 3.0
        "kick40.rom",  // Kickstart 4.0
        nullptr
    };

    for (int i = 0; kickstarts[i]; i++) {
        std::string biosPath = std::string(kBiosDir) + "/" + kickstarts[i];
        if (fileExists(biosPath)) {
            LOGI("Selected Kickstart: %s", kickstarts[i]);
            return biosPath;
        }
    }

    return "";  // No Kickstart found
}

// Validate that the game file is a valid Amiga disk image
bool isValidAmigaDiskImage(const std::string& path) {
    std::string e = ext(path);
    if (e != ".adf" && e != ".hdf" && e != ".dms") {
        return false;
    }

    // Basic size validation
    long size = fileSize(path);
    if (size < 0) return false;

    // ADF files are typically 880KB (floppy) or larger
    // HDF files can be much larger (hard disk images)
    // DMS files are compressed, so size varies
    if (e == ".adf" && size < 100000) return false;  // Too small for ADF
    if (e == ".hdf" && size < 100000) return false;  // Too small for HDF

    return true;
}

// Generate a UAE config for running Amiga games
std::string generateUaeConfig(const std::string& gamePath, const std::string& biosPath) {
    std::string gameDir = gamePath.substr(0, gamePath.rfind('/') + 1);
    std::string gameFile = gamePath.substr(gamePath.rfind('/') + 1);
    std::string e = ext(gamePath);

    // Build UAE configuration
    std::string config = R"([general]
fullscreen=false
width=640
height=512
amiga_model=A500
cpu_speed=fastest
cpu_type=68000
fpu_type=none
chipset=ocs
chipram=2
fastram=0
bogomem=0
z3fastram=0

[display]
framerate=50
vsync=true
linemode=scanlines
aspect=true

[sound]
sound=true
frequency=44100
channels=2
volume=100

[input]
joystick_type=automatic
mouse_speed=100

[harddrives]
)";

    // Add disk configuration
    if (e == ".adf") {
        config += "floppy0=" + gamePath + "\n";
        config += "floppy0type=3.5_DD\n";  // 3.5" double density
    } else if (e == ".hdf" || e == ".dms") {
        config += "hardfile0=" + gamePath + "\n";
    }

    config += R"(
[cpu]
cpu_cycle_exact=false
cpu_compatible=true

[blitter]
blitter_cycle_exact=false
blitter_compatible=true
)";

    return config;
}

AmigaLaunchResult LaunchAmigaGame(const std::string& romPath) {
    if (romPath.empty())
        return {false, "Amiga launch failed: empty ROM path", "", ""};

    // Validate the game file
    if (!fileExists(romPath)) {
        return {false, "Amiga launch failed: game file not found: " + romPath, "", ""};
    }

    if (!isValidAmigaDiskImage(romPath)) {
        return {false, 
            "Amiga launch failed: invalid disk image format. Expected .adf, .hdf, or .dms file.\n"
            "For Dune II, use the .adf (floppy) or .hdf (hard disk) version.",
            "", ""};
    }

    // Find a suitable Kickstart ROM
    std::string biosPath = selectKickstartRom(romPath);
    if (biosPath.empty()) {
        return {false, 
            "Amiga launch failed: No Kickstart ROM found in " + std::string(kBiosDir) + "/\n"
            "Required: kick13.rom, kick31.rom, or other Kickstart versions.\n"
            "Place legally dumped Kickstart ROMs in the system directory.",
            "", ""};
    }

    // Validate Kickstart ROM size (should be 262144 bytes for 256KB or 524288 for 512KB)
    long biosSize = fileSize(biosPath);
    if (biosSize != 262144L && biosSize != 524288L) {
        LOGI("Warning: Kickstart ROM size is %ld (expected 262144 or 524288)", biosSize);
        // Don't fail, as some variants may have different sizes
    }

    // Generate UAE configuration
    std::string uaeConfig = generateUaeConfig(romPath, biosPath);

    LOGI("Amiga ready: rom=%s bios=%s config_size=%zu", 
         romPath.c_str(), biosPath.c_str(), uaeConfig.size());

    return {true, 
            "Amiga core ready for " + romPath + " using " + biosPath,
            romPath, 
            biosPath};
}

}  // namespace retrorts::amiga
