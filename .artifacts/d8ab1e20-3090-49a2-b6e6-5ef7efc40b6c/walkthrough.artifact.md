# Walkthrough - Fix "dune: file is not executable"

I have implemented the fixes to resolve the "file is not executable" error and improved the detection of extensionless game files like "Dune".

## Changes Made

### 1. Improved Game Detection
- **[GameProfile.kt](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/java/com/retrorts/ui/GameProfile.kt)**
    - Updated `ConsoleType.detect` to use folder-based heuristics for extensionless files. Files in `/amiga/` now correctly default to the `AMIGA` engine instead of `DOSBOX`.
    - Refined `gameIdForName` to map `dune` (extensionless) specifically to the Amiga preset.

### 2. Support for Extensionless Files in Library
- **[GameLibrary.kt](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/java/com/retrorts/GameLibrary.kt)**
    - Updated `scanGamesFolder` to recognize files without extensions if they are located within a recognized console subdirectory (e.g., `RetroRTS/Games/Amiga/`).

### 3. Native Engine Compatibility
- **[emulator_core.cpp](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/cpp/emulator_core.cpp)**
    - Expanded the DOSBox `autoexec` logic to check for `dune` (no extension), `dune.exe`, and `dune.bat`. This ensures that if a DOS version of Dune is launched, it starts correctly even without a complex config.

### 4. Automatic Permission Fixing
- **[MainActivity.kt](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/java/com/retrorts/MainActivity.kt)**
    - Added an "Auto-fix" mechanism during game launch. The app now explicitly sets the `executable` and `readable` bits on the game file via the Java/Kotlin `File` API before the emulator starts. This resolves the `file is not executable` error seen in the Amiga shell.

## Verification Results

### Automated Tests
- Successfully ran `:RetroRTS:app:assembleDebug` to verify compilation of both Kotlin and C++ changes.

### Manual Verification Recommended
> [!IMPORTANT]
> To verify the fix:
> 1. Place your `dune` file (without an extension) in `/sdcard/RetroRTS/Games/Amiga/`.
> 2. Open RetroRTS and check if it appears in the Library with the Amiga (💾) icon.
> 3. Launch the game. The "file is not executable" error should no longer appear, as the app now fixes the host permissions automatically.
