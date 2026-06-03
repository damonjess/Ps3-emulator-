#pragma once
#include <string>

namespace retrorts::ps1 {

struct Ps1LaunchResult {
    bool        ok;
    std::string message;
    std::string resolvedCuePath;   // path to use when calling the emulator
};

Ps1LaunchResult LaunchPs1Game(const std::string& discPath);

}  // namespace retrorts::ps1
