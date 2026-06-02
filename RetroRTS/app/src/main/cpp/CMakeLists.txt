cmake_minimum_required(VERSION 3.22.1)
project(retrorts_dosbox LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(NOT ANDROID_ABI STREQUAL "arm64-v8a")
    message(FATAL_ERROR "This build is configured for arm64-v8a only. Current ABI: ${ANDROID_ABI}")
endif()

# -----------------------------------------------------------------------------
# Fetch and Embed a Lightweight DOSBox Core
# -----------------------------------------------------------------------------
include(FetchContent)

FetchContent_Declare(
    dosbox_core
    GIT_REPOSITORY https://github.com/joncampbell123/dosbox-x.git
    GIT_TAG        master # For a stable release, pin a specific commit hash here
)
FetchContent_MakeAvailable(dosbox_core)

# -----------------------------------------------------------------------------
# Define Shared JNI Emulator Library
# -----------------------------------------------------------------------------
add_library(retrorts_dosbox_jni SHARED
    dosbox_jni.cpp
    dosbox_audio.cpp
    emulator_core.cpp
    dsi_core.cpp
    native-lib.cpp
)

# Ensure the fetched DOSBox headers are visible to your JNI code
target_include_directories(retrorts_dosbox_jni PRIVATE
    ${dosbox_core_SOURCE_DIR}/include
    ${dosbox_core_SOURCE_DIR}/src
)

find_library(log-lib log)
find_library(android-lib android)
find_library(aaudio-lib aaudio)

target_link_libraries(retrorts_dosbox_jni
    dosbox_core_lib # Binds the fetched emulator engine into your JNI layer
    ${log-lib}
    ${android-lib}
    ${aaudio-lib}
)