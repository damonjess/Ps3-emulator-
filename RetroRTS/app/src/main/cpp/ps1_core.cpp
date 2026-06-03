#include "ps1_core.h"
#include <fstream>
#include <algorithm>
#include <string>
#include <cctype>

namespace retrorts::ps1 {
namespace {

constexpr const char* kPs1SystemDir = "/sdcard/RetroRTS/system/ps1";
constexpr const char* kPs1BiosFile = "scph1001.bin";

bool FileExists(const std::string& path) {
    std::ifstream file(path, std::ios::binary);
    return file.good();
}

bool HasPs1CompatibleExtension(std::string romPath) {
    std::transform(romPath.begin(), romPath.end(), romPath.begin(), [](unsigned char c) {
        return static_cast<char>(std::tolower(c));
    });

    return romPath.ends_with(".cue") || romPath.ends_with(".bin") ||
           romPath.ends_with(".iso") || romPath.ends_with(".pbp") ||
           romPath.ends_with(".chd");
}

} // namespace

Ps1LaunchResult LaunchPs1Game(const std::string& romPath) {
    if (romPath.empty()) {
        return {false, "PS1 launch failed: empty ROM path"};
    }

    if (!HasPs1CompatibleExtension(romPath)) {
        return {false, "PS1 launch failed: expected .cue, .bin, .iso, .pbp, or .chd game file"};
    }

    if (!FileExists(romPath)) {
        return {false, "PS1 launch failed: game file does not exist: " + romPath};
    }

    const std::string biosPath = std::string(kPs1SystemDir) + "/" + kPs1BiosFile;
    if (!FileExists(biosPath)) {
        return {false, "PS1 launch failed: missing BIOS file: " + biosPath};
    }

    return {true, "PS1 core (PCSX-ReARMed) ready for " + romPath};
}

} // namespace retrorts::ps1
