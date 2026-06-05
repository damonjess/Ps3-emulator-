#pragma once
#include <string>

namespace retrorts::amiga {

struct AmigaLaunchResult {
    bool        ok;
    std::string message;
    std::string resolvedRomPath;
    std::string resolvedBiosPath;
};

AmigaLaunchResult LaunchAmigaGame(const std::string& romPath);

}  // namespace retrorts::amiga
