#include "emulator_core.h"

namespace retrorts {

std::string LaunchGame(const std::string& console, const std::string& romPath) {
    return "[Native] Booting " + console + " game from: " + romPath;
}

}  // namespace retrorts
