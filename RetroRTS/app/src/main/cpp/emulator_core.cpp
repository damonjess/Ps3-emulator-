#include "emulator_core.h"

#include "dsi_core.h"

namespace retrorts {

std::string LaunchGame(const std::string& console, const std::string& romPath) {
    if (console == "NINTENDO_DSI") {
        const dsi::DsiLaunchResult result = dsi::LaunchDsiGame(romPath);
        return result.ok ? result.message : "ERROR: " + result.message;
    }

    return "[Native] Booting " + console + " game from: " + romPath;
}

}  // namespace retrorts
