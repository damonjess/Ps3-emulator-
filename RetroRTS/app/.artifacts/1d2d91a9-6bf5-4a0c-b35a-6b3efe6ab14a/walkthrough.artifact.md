# Walkthrough - Build Fixes for Libretro Bridge

I have fixed the build errors in the `RetroRTS` module. The issues were related to the C++ bridge implementation for Libretro.

## Changes Made

### Native Code

#### [libretro.h](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/cpp/libretro.h)
- Added the missing `RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY` constant (value `9`). This constant is required for the core to request the BIOS/system directory path from the host.

#### [libretro_bridge.cpp](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/cpp/libretro_bridge.cpp)
- Fixed the function pointer cast for `retro_set_input_poll`. The previous cast was missing the argument type `retro_input_poll_t`, causing a compilation error.

## Verification Results

### Automated Tests
- Ran `:RetroRTS:app:assembleDebug` and it finished successfully.
