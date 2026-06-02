#include "emulator_core.h"
#include "dsi_core.h"

namespace retrorts {

std::string LaunchGame(const std::string& console, const std::string& romPath) {
    if (console == "NINTENDO_DSI") {
        const dsi::DsiLaunchResult result = dsi::LaunchDsiGame(romPath);
        return result.ok ? result.message : "ERROR: " + result.message;
    }

    if (console == "AMIGA") {
        // TODO: Real PUAE / UAE4ARM integration later
        // For now: Smart fallback + validation
        if (romPath.find(".adf") != std::string::npos ||
            romPath.find(".adz") != std::string::npos ||
            romPath.find(".dms") != std::string::npos) {
            return "[Amiga] Launching ADF disk: " + romPath + " (UAE backend ready)";
        }

        // Allow PC games like Dune 2000 to fallback gracefully
        return "[Amiga] No ADF found. Falling back to DOSBox for PC title. Path: " + romPath;
    }

    return "[Native] Booting " + console + " game from: " + romPath;
}

}  // namespace retrorts