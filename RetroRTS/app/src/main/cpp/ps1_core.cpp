#include "ps1_core.h"

#include <algorithm>
#include <cctype>
#include <fstream>
#include <sstream>
#include <string>
#include <android/log.h>

#define LOG_TAG "RetroRTS_PS1"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace retrorts::ps1 {
namespace {

constexpr const char* kBiosDir  = "/sdcard/RetroRTS/system/ps1";
constexpr const char* kBiosFile = "scph1001.bin";   // most compatible BIOS

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

// Generate a minimal .cue for a single-track .bin in a writable cache directory
std::string generateCue(const std::string& binPath, const std::string& cacheDir) {
    // Extract filename from path
    std::string filename = binPath.substr(binPath.rfind('/') + 1);

    // Create a unique name for the cue in cache to avoid name collisions
    std::string cueName = filename;
    auto dot = cueName.rfind('.');
    if (dot != std::string::npos) cueName = cueName.substr(0, dot);
    cueName += ".cue";

    std::string cuePath = cacheDir + "/" + cueName;

    // Check if it already exists and points to the same bin
    std::ifstream existing(cuePath);
    if (existing.good()) {
        std::string line;
        while (std::getline(existing, line)) {
            if (line.find(filename) != std::string::npos) return cuePath;
        }
    }
    existing.close();

    std::ofstream cue(cuePath);
    if (!cue.good()) return "";

    // Important: PCSX-ReARMed needs the path to the bin if the cue isn't in the same folder.
    // However, the standard .cue format 'FILE "name.bin" BINARY' expects it in the same folder.
    // If we put the .cue in cache and .bin is in Games, we should use the absolute path in the .cue.
    cue << "FILE \"" << binPath << "\" BINARY\n"
        << "  TRACK 01 MODE2/2352\n"
        << "    INDEX 01 00:00:00\n";
    cue.close();
    LOGI("Generated cue in cache: %s", cuePath.c_str());
    return cuePath;
}

}  // namespace

Ps1LaunchResult LaunchPs1Game(const std::string& discPath, const std::string& cacheDir) {
    if (discPath.empty())
        return {false, "PS1 launch failed: empty disc path", ""};

    const std::string e = ext(discPath);

    // ── Validate extension ───────────────────────────────────────────────
    if (e != ".bin" && e != ".cue" && e != ".img" && e != ".iso")
        return {false, "PS1 launch failed: expected .bin, .cue, .img, or .iso — got: " + discPath, ""};

    // ── Check disc file exists ───────────────────────────────────────────
    if (!fileExists(discPath))
        return {false, "PS1 launch failed: disc file not found: " + discPath, ""};

    // ── Check BIOS ───────────────────────────────────────────────────────
    const std::string biosPath = std::string(kBiosDir) + "/" + kBiosFile;
    bool hasBios = fileExists(biosPath);

    if (hasBios) {
        const long biosSize = fileSize(biosPath);
        if (biosSize != 524288L) {
            LOGE("BIOS file exists but size is wrong: %ld", biosSize);
            hasBios = false; // Fallback to HLE if BIOS is corrupt
        }
    }

    if (!hasBios) {
        LOGI("BIOS not found, using PS1 HLE (High Level Emulation)");
    }

    // ── Resolve to .cue (generate one if only .bin provided) ────────────
    std::string cuePath;
    if (e == ".cue") {
        cuePath = discPath;
        // Make sure the .bin it references exists (basic check)
        std::ifstream cueFile(discPath);
        std::string line;
        while (std::getline(cueFile, line)) {
            if (toLower(line).find("file") != std::string::npos) {
                // Extract filename/path from:  FILE "name.bin" BINARY
                auto q1 = line.find('"');
                auto q2 = line.rfind('"');
                if (q1 != std::string::npos && q2 > q1) {
                    std::string binRef = line.substr(q1 + 1, q2 - q1 - 1);
                    // Check if it's a relative ref or absolute
                    if (binRef.find('/') == std::string::npos) {
                        std::string dir = discPath.substr(0, discPath.rfind('/') + 1);
                        if (!fileExists(dir + binRef))
                            return {false, "PS1 launch failed: .cue references missing file: " + binRef, ""};
                    } else {
                         if (!fileExists(binRef))
                            return {false, "PS1 launch failed: .cue references missing file: " + binRef, ""};
                    }
                }
            }
        }
    } else if (e == ".bin" || e == ".img") {
        cuePath = generateCue(discPath, cacheDir);
        if (cuePath.empty())
            return {false, "PS1 launch failed: could not create .cue for " + discPath, ""};
    } else {
        // .iso — pass directly, PCSX-ReARMed accepts ISO
        cuePath = discPath;
    }

    LOGI("PS1 ready: disc=%s bios=%s", cuePath.c_str(), hasBios ? biosPath.c_str() : "HLE");
    return {true, "PS1 core ready — " + cuePath, cuePath, hasBios ? biosPath : "HLE"};
}

}  // namespace retrorts::ps1
