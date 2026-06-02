#include "dsi_core.h"

#include <algorithm>
#include <array>
#include <cctype>
#include <fstream>
#include <sstream>
#include <string>

namespace retrorts::dsi {
namespace {

constexpr const char* kSystemDir = "/sdcard/RetroRTS/system/dsi";
constexpr std::array<const char*, 4> kRequiredSystemFiles = {
    "bios7.bin",
    "bios9.bin",
    "firmware.bin",
    "nand.bin",
};

bool FileExists(const std::string& path) {
    std::ifstream file(path, std::ios::binary);
    return file.good();
}

bool HasDsiCompatibleExtension(std::string romPath) {
    std::transform(romPath.begin(), romPath.end(), romPath.begin(), [](unsigned char c) {
        return static_cast<char>(std::tolower(c));
    });

    return romPath.ends_with(".nds") || romPath.ends_with(".dsi") || romPath.ends_with(".srl");
}

}  // namespace

DsiLaunchResult LaunchDsiGame(const std::string& romPath) {
    if (romPath.empty()) {
        return {false, "Nintendo DSi launch failed: empty ROM path"};
    }

    if (!HasDsiCompatibleExtension(romPath)) {
        return {false, "Nintendo DSi launch failed: expected .nds, .dsi, or .srl game file"};
    }

    if (!FileExists(romPath)) {
        return {false, "Nintendo DSi launch failed: game file does not exist: " + romPath};
    }

    std::ostringstream missing;
    bool hasMissingFile = false;
    for (const char* fileName : kRequiredSystemFiles) {
        const std::string path = std::string(kSystemDir) + "/" + fileName;
        if (!FileExists(path)) {
            if (hasMissingFile) {
                missing << ", ";
            }
            missing << fileName;
            hasMissingFile = true;
        }
    }

    if (hasMissingFile) {
        return {
            false,
            "Nintendo DSi launch failed: missing system files in " + std::string(kSystemDir) + ": " + missing.str(),
        };
    }

    return {
        true,
        "Nintendo DSi core ready for " + romPath + " using system files from " + std::string(kSystemDir),
    };
}

}  // namespace retrorts::dsi
