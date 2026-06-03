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

// Generate a minimal .cue for a single-track .bin
std::string generateCue(const std::string& binPath) {
    // cue sits next to the bin, same name but .cue extension
    auto pos = binPath.rfind('.');
    if (pos == std::string::npos) return "";
    std::string cuePath = binPath.substr(0, pos) + ".cue";
    if (fileExists(cuePath)) return cuePath;   // already exists

    // Extract just the filename for the FILE line
    std::string filename = binPath.substr(binPath.rfind('/') + 1);

    std::ofstream cue(cuePath);
    if (!cue.good()) return "";
    cue << "FILE \"" << filename << "\" BINARY\n"
        << "  TRACK 01 MODE2/2352\n"
        << "    INDEX 01 00:00:00\n";
    cue.close();
    LOGI("Generated cue: %s", cuePath.c_str());
    return cuePath;
}

}  // namespace

Ps1LaunchResult LaunchPs1Game(const std::string& discPath) {
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
    if (!fileExists(biosPath))
        return {false,
                "PS1 launch failed: BIOS not found.\n"
                "Place scph1001.bin in /sdcard/RetroRTS/system/ps1/",
                ""};

    const long biosSize = fileSize(biosPath);
    if (biosSize != 524288L)   // PS1 BIOS is always exactly 512 KB
        return {false,
                "PS1 launch failed: scph1001.bin looks corrupt (expected 512 KB, got "
                + std::to_string(biosSize / 1024) + " KB)",
                ""};

    // ── Resolve to .cue (generate one if only .bin provided) ────────────
    std::string cuePath;
    if (e == ".cue") {
        cuePath = discPath;
        // Make sure the .bin it references exists (basic check)
        std::ifstream cueFile(discPath);
        std::string line;
        while (std::getline(cueFile, line)) {
            if (toLower(line).find("file") != std::string::npos &&
                toLower(line).find(".bin") != std::string::npos) {
                // Extract filename from:  FILE "name.bin" BINARY
                auto q1 = line.find('"');
                auto q2 = line.rfind('"');
                if (q1 != std::string::npos && q2 > q1) {
                    std::string binName = line.substr(q1 + 1, q2 - q1 - 1);
                    std::string dir     = discPath.substr(0, discPath.rfind('/') + 1);
                    if (!fileExists(dir + binName))
                        return {false,
                                "PS1 launch failed: .cue references missing file: " + binName, ""};
                }
            }
        }
    } else if (e == ".bin" || e == ".img") {
        cuePath = generateCue(discPath);
        if (cuePath.empty())
            return {false, "PS1 launch failed: could not create .cue for " + discPath, ""};
    } else {
        // .iso — pass directly, PCSX-ReARMed accepts ISO
        cuePath = discPath;
    }

    LOGI("PS1 ready: disc=%s bios=%s", cuePath.c_str(), biosPath.c_str());
    return {true, "PS1 core ready — " + cuePath, cuePath};
}

}  // namespace retrorts::ps1
