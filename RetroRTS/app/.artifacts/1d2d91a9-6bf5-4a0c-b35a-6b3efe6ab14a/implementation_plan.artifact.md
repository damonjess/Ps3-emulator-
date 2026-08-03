# Implementation Plan - Fix Build Issues in Libretro Bridge

The project is currently failing to build due to C++ compilation errors in `libretro_bridge.cpp`. These errors are caused by an incorrect function pointer cast and a missing macro definition in `libretro.h`.

## User Review Required

> [!IMPORTANT]
> I am adding a missing macro `RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY` to `libretro.h` with value `9`, which is the standard value in the libretro API.

## Proposed Changes

### Native Build (C++)

#### [MODIFY] [libretro.h](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/cpp/libretro.h)
- Add `#define RETRO_ENVIRONMENT_GET_SYSTEM_DIRECTORY 9` to provide the missing constant.

#### [MODIFY] [libretro_bridge.cpp](file:///C:/Users/Damon/StudioProjects/Retro-app/RetroRTS/app/src/main/cpp/libretro_bridge.cpp)
- Fix the function pointer cast for `retro_set_input_poll_fn` from `(void (*)())` to `(void (*)(retro_input_poll_t))`.

## Verification Plan

### Automated Tests
- Run `:RetroRTS:app:assembleDebug` to verify that the project now builds successfully.
