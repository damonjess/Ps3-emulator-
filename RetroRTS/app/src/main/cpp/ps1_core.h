#pragma once
#include <string>

namespace retrorts::ps1 {

struct Ps1LaunchResult {
    bool        ok;
    std::string message;
    std::string resolvedCuePath;   // path to use when calling the emulator
    std::string resolvedBiosPath;  // path to bios or "HLE"
};

Ps1LaunchResult LaunchPs1Game(const std::string& discPath, const std::string& cacheDir);

}  // namespace retrorts::ps1
