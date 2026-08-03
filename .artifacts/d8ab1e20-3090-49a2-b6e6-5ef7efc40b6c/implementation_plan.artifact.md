# Implementation Plan - Fix "dune: file is not executable" Error

The user is encountering an error `dune: file is not executable` when attempting to run a game or tool named "dune" within the emulator. The screenshot shows an Amiga-style environment (ARDS / Amiga shell). This error typically indicates that the "Executable" (E) protection bit is missing from the file, a common issue when files are transferred from modern filesystems to retro environments.

Additionally, the current codebase has a potential misdetection bug where files without extensions (like `dune`) default to DOSBox, even if they are intended for the Amiga emulator.

## Proposed Changes

### [Component] Game Engine & Detection

#### [MODIFY] [GameProfile.kt](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/java/com/retrorts/ui/GameProfile.kt)
- **Smarter Detection**: Update `ConsoleType.detect()` to handle files without extensions more intelligently. If a file is in an `Amiga` directory or contains Amiga-specific metadata patterns, default to `AMIGA` instead of `DOSBOX`.
- **Dune Heuristics**: Distinguish between `dune` (Amiga/Original) and `dune2000` (DOS) more accurately in `gameIdForName`.

#### [MODIFY] [GameLibrary.kt](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/java/com/retrorts/GameLibrary.kt)
- **Scan Extensionless Files**: Update `scanGamesFolder` to include files with no extension if they are located in recognized console subdirectories. This allows games like "Dune" (Amiga) to be discovered even if they lack an `.adf` extension.

### [Component] Native Core Bridge

#### [MODIFY] [emulator_core.cpp](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/cpp/emulator_core.cpp)
- **DOSBox Autoexec**: Expand the `autoexec` block to check for `dune` (no extension) and other common variants.
- **Amiga Configuration**: Ensure that when launching Amiga games, the correct model and memory settings are applied (currently hardcoded to A500 in the bridge).

### [Component] UI & Permission Utility

#### [MODIFY] [MainActivity.kt](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/java/com/retrorts/MainActivity.kt)
- **Permission Fixer**: Add a utility to the `GameCard` to "Fix Permissions". This will:
    1. Set the Android `executable` bit on the host file.
    2. (Future) Attempt to inject a `protect [file] +e` command if the core supports a startup script.
- **Auto-Fix on Launch**: Attempt to set the host executable bit automatically when a game is launched from the library.

## Verification Plan

### Automated Tests
- Run `BridgeAndValidationTest.kt` to ensure detection logic still works for standard extensions.
- Add new test cases for extensionless files like `dune`.

### Manual Verification
1. Place a file named `dune` in `/sdcard/RetroRTS/Games/Amiga/`.
2. Launch the app and verify it appears in the Library as an Amiga game.
3. Launch the game and verify the "file is not executable" error is resolved (either by correct core selection or permission fixing).
