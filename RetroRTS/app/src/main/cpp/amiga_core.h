#pragma once
#include <string>

namespace retrorts::amiga {

/**
 * AmigaLaunchResult encapsulates the result of launching an Amiga game.
 * 
 * Fields:
 * - ok: true if the game is ready to launch
 * - message: human-readable status or error message
 * - resolvedRomPath: validated path to the game disk image (.adf, .hdf, .dms)
 * - resolvedBiosPath: validated path to the Kickstart ROM
 */
struct AmigaLaunchResult {
    bool        ok;
    std::string message;
    std::string resolvedRomPath;
    std::string resolvedBiosPath;
};

/**
 * LaunchAmigaGame validates and prepares an Amiga game for execution.
 * 
 * This function:
 * 1. Validates the game disk image (.adf, .hdf, .dms)
 * 2. Locates an appropriate Kickstart ROM
 * 3. Generates a UAE configuration for the game
 * 4. Returns paths to both the game and BIOS
 * 
 * The caller should use the returned paths to invoke the UAE emulator backend.
 * 
 * @param romPath Full path to the Amiga game disk image
 * @return AmigaLaunchResult with status and resolved paths
 */
AmigaLaunchResult LaunchAmigaGame(const std::string& romPath);

}  // namespace retrorts::amiga
