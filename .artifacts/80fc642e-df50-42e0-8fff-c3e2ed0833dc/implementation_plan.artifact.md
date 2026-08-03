# Implementation Plan - Libretro Wrapper Bridge

This plan addresses the need for a "Libretro wrapper bridge" that allows the existing emulator JNI calls (which expect legacy symbols like `PCSX_Run`, `uae_init`, and `dosbox_init`) to work with standard Libretro cores (which export `retro_*` symbols).

## User Review Required

> [!IMPORTANT]
> The bridge will intercept calls to `PCSX_Run`, `uae_init`, and `dosbox_init` and redirect them to a common Libretro host. This means the app will now be able to use standard Libretro cores (`.so` files) for PS1, Amiga, and DOS.

## Proposed Changes

### Core Bridge Component

#### [NEW] [libretro.h](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/cpp/libretro.h)
Standard Libretro API header to allow interaction with Libretro cores.

#### [NEW] [libretro_bridge.h](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/cpp/libretro_bridge.h)
Header defining the `LibretroHost` class and legacy bridge exports.

#### [NEW] [libretro_bridge.cpp](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/cpp/libretro_bridge.cpp)
Implementation of the `LibretroHost` and the bridge functions:
- `PCSX_Run`: Maps PS1 requests to Libretro core.
- `uae_init`: Maps Amiga requests to Puae Libretro core.
- `dosbox_init`: Maps DOS requests to Dosbox Pure Libretro core.

### Emulator Integration

#### [MODIFY] [emulator_core.cpp](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/cpp/emulator_core.cpp)
Update core loading logic to use the new bridge functions instead of attempting to `dlsym` legacy symbols directly from the Libretro cores (which don't have them).

#### [MODIFY] [amiga_uae_bridge_jni.cpp](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/cpp/amiga_uae_bridge_jni.cpp)
Update JNI calls to use the bridge functions.

#### [MODIFY] [dosbox_bridge_jni.cpp](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/cpp/dosbox_bridge_jni.cpp)
Update JNI calls to use the bridge functions.

### Build System

#### [MODIFY] [CMakeLists.txt](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/cpp/CMakeLists.txt)
Add `libretro_bridge.cpp` to the build and ensure symbols are correctly exported.

---

## Verification Plan

### Automated Tests
- `gradle_build(":RetroRTS:app:assembleDebug")` to verify compilation.

### Manual Verification
- Deploy the app and attempt to launch a PS1, Amiga, or DOS game.
- Verify that the Libretro core is loaded and `retro_run` loop starts.
- Check logs for "Libretro bridge" initialization messages.
