#pragma once

#include <string>

namespace retrorts::dsi {

struct DsiLaunchResult {
    bool ok;
    std::string message;
};

DsiLaunchResult LaunchDsiGame(const std::string& romPath);

}  // namespace retrorts::dsi
