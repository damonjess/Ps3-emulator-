# Implementation Plan - Fix Libretro Core Crash (Amiga/PUAE)

The app crashes when launching "Dune" (Amiga) during the "Loading Engine" phase. The crash is a `SIGSEGV` (null pointer dereference) in `libpuae.so` during `retro_init`, specifically within a `vfprintf` call. This indicates the core is attempting to log messages but the environment has not provided a valid logging interface.

## User Review Required

> [!IMPORTANT]
> The crash is caused by the Libretro bridge missing essential environment callbacks that modern cores like PUAE expect. Specifically, providing a logging interface is critical for stability.

- I will implement a native logging bridge that redirects Libretro core logs to Android Logcat.
- I will also implement "System" and "Save" directory callbacks to ensure cores know where to find BIOS and store data.

## Proposed Changes

### Libretro Headers
#### [MODIFY] [libretro.h](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/cpp/libretro.h)
- Add missing definitions for:
    - `retro_log_level` enum
    - `retro_log_printf_t` typedef
    - `retro_log_callback` struct
    - `RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY` and other missing command IDs if needed (though some are already there).

### Libretro Bridge
#### [MODIFY] [libretro_bridge.h](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/cpp/libretro_bridge.h)
- Add `void setSystemDir(const std::string& dir)` and `void setSaveDir(const std::string& dir)` to `LibretroHost`.
- Add private members to store these paths.

#### [MODIFY] [libretro_bridge.cpp](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/cpp/libretro_bridge.cpp)
- Implement a static `retro_log_printf` function that uses `__android_log_vprint`.
- Update `envCallback` to handle:
    - `RETRO_ENVIRONMENT_GET_LOG_INTERFACE`: Fill the provided struct with our logging function.
    - `RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY`: Return the stored system directory path.
    - `RETRO_ENVIRONMENT_GET_SAVE_DIRECTORY`: Return the stored save directory path.
    - `RETRO_ENVIRONMENT_GET_CORE_ASSETS_DIRECTORY`: Fallback to system directory.

### UI / MainActivity
#### [MODIFY] [MainActivity.kt](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/java/com/retrorts/MainActivity.kt)
- Update `onCreate` to set the system and save directories in the bridge using app-specific external storage paths.

## Verification Plan

### Automated Verification
- Rebuild the native components to ensure `libretro.h` changes don't break existing cores.

### Manual Verification
- Launch Dune (Amiga).
- Verify that the app no longer crashes during "Loading Engine".
- Check Logcat for tags like `LibretroCore` to see logs coming from `libpuae.so`.
