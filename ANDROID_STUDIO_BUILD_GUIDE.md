# Android Studio Build Guide for RetroRTS

This guide provides step-by-step instructions to build the RetroRTS emulator project in Android Studio, ensuring all native cores (Amiga, PS1, DOSBox) are properly integrated.

## Prerequisites

1.  **Android Studio**: Hedgehog (2023.1.1) or newer recommended.
2.  **Android SDK**: API 34 (Android 14) platform installed.
3.  **Android NDK**: Version 25.1.8937393 or newer (install via SDK Manager > SDK Tools > NDK (Side by side)).
4.  **CMake**: Version 3.22.1 or newer.
5.  **JDK**: Java 17 (bundled with Android Studio).

## Step 1: Open the Project

1.  Launch Android Studio.
2.  Select **Open** and navigate to the root folder where you cloned the repository (`Ps3-emulator-`).
3.  Wait for the Gradle sync to complete. This may take a few minutes as it downloads dependencies.

## Step 2: Configure Native Libraries (Crucial)

The repository contains the "bridge" code, but for the emulators to actually run games, you need to provide the native core libraries.

### 1. Amiga (UAE)
- You need `libpuae.so`.
- Create the directory: `RetroRTS/app/src/main/jniLibs/arm64-v8a/`
- Place your compiled `libpuae.so` (for ARM64) into that folder.

### 2. DOSBox
- You need `libdosbox_pure.so`.
- Place `libdosbox_pure.so` into the same `jniLibs/arm64-v8a/` folder.

### 3. PS1 (PCSX-ReARMed)
- The project is configured to build `pcsx_core` from source.
- If you have the full PCSX-ReARMed source, place it in `RetroRTS/app/src/main/cpp/pcsx_rearmed/`.
- If not, the build will use the included `pcsx_android_stubs.c` which allows the app to compile but won't run PS1 games yet.

## Step 3: Set Up Your Device/Emulator

1.  **Physical Device (Recommended)**:
    - Enable **Developer Options** and **USB Debugging** on your Android phone.
    - Connect it to your computer via USB.
    - Note: Native emulators perform significantly better on real hardware than in the Android Studio Emulator.
2.  **Android Studio Emulator**:
    - If using an emulator, ensure it is an **x86_64** image with **Google Play APIs**.
    - Note: You will need to provide `x86_64` versions of the `.so` libraries in `jniLibs/x86_64/`.

## Step 4: Build and Run

1.  In the top toolbar, ensure the **app** module is selected.
2.  Select your device from the dropdown.
3.  Click the **Run** button (Green Play icon) or press `Shift + F10`.
4.  Android Studio will:
    - Compile the C++ code using CMake and the NDK.
    - Compile the Kotlin/Java code.
    - Package everything into an APK.
    - Install and launch the app on your device.

## Step 5: Post-Installation Setup

Once the app is running on your device, you must set up the directory structure for your games and BIOS:

1.  **Grant Permissions**: When prompted, allow the app to access all files (required for reading ROMs and BIOS).
2.  **BIOS Files**:
    - **Amiga**: Place `kick13.rom` in `/sdcard/RetroRTS/system/amiga/`.
    - **PS1**: Place `scph1001.bin` in `/sdcard/RetroRTS/system/ps1/`.
3.  **Games**:
    - **Amiga**: Place `.adf` files (like Dune II) in `/sdcard/RetroRTS/Games/Amiga/`.
    - **DOSBox**: Place game folders in `/sdcard/RetroRTS/Games/DOSBox/`.
    - **PS1**: Place `.bin/.cue` files in `/sdcard/RetroRTS/Games/PS1/`.

## Troubleshooting Build Errors

### "CMake Error: The source directory ... does not exist"
- This usually means a path in `CMakeLists.txt` is wrong. Ensure your folder structure matches the expected structure.

### "NDK Resolution Failed"
- Go to **File > Project Structure > SDK Location** and ensure the **Android NDK location** is correctly set.

### "Library not found: libpuae.so"
- Ensure the `.so` file is in the correct `jniLibs` subdirectory for your device's architecture (usually `arm64-v8a`).

### Gradle Sync Fails
- Try **File > Invalidate Caches / Restart** and then **Build > Clean Project**.

## How to Verify Amiga Support for Dune II

1.  Launch the app.
2.  The "Amiga" console should be visible.
3.  If you have placed `dune_ii.adf` in the correct folder, it will appear in the game list.
4.  Tap the game. The app will generate the UAE config and attempt to load the `libpuae.so` library to start the game.
