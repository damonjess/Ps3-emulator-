# PS1 + PS2 Android Emulator (Advanced Prototype)

This project is now oriented around a **combined PS1 and PS2 emulator shell for Android**.

## Included now

- Runtime console selection: **PS1** or **PS2**.
- Virtual on-screen gamepad with D-pad, face buttons, shoulders, start/select/PS, L3/R3.
- Dual analog sticks with normalized `[-1, 1]` output.
- Core lifecycle controls: start, pause, resume, stop.
- JNI-ready native bridge for plugging in a native emulator backend.

## Important scope note

A real PS1/PS2 emulator still requires significant native systems work (CPU emulation/JIT, GPU emulation, timing/sync, audio, I/O, compatibility testing).
This repository provides the Android app/input architecture and backend contract.

## Key files

- `app/src/main/java/com/example/ps3emu/core/EmulatorCore.kt`
- `app/src/main/java/com/example/ps3emu/core/MockPs3Core.kt`
- `app/src/main/java/com/example/ps3emu/core/NativePs3Core.kt`
- `app/src/main/java/com/example/ps3emu/viewmodel/EmulatorViewModel.kt`
- `app/src/main/java/com/example/ps3emu/ui/EmulatorScreen.kt`
- `app/src/main/java/com/example/ps3emu/ui/VirtualGamepad.kt`

## Next steps

1. Implement native `psxcore` JNI functions for PS1 + PS2 execution backends.
2. Add BIOS/game-loader flow and storage permissions UX.
3. Add Vulkan/OpenGL renderer path and audio pipeline.
