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
    GIT_REPOSITORY https://github.com/realnc/dosbox-core.git
    GIT_TAG        master
)
FetchContent_GetProperties(dosbox_core)
if(NOT dosbox_core_POPULATED)
    FetchContent_Populate(dosbox_core)
endif()

# Add the directory containing SDL stubs to global include path
# We use include_directories to ensure it's applied to all targets defined here
include_directories(BEFORE "${CMAKE_CURRENT_SOURCE_DIR}")

# Since the upstream repo doesn't have a root CMakeLists.txt, we define the target here.
# We collect the necessary sources from the fetched directory.
file(GLOB_RECURSE DOSBOX_SOURCES
    "${dosbox_core_SOURCE_DIR}/src/*.cpp"
    "${dosbox_core_SOURCE_DIR}/src/*.c"
)

# Exclude unnecessary files and those that might conflict
# We use a more aggressive exclusion list to avoid SDL dependencies
file(GLOB_RECURSE EXCLUDE_SOURCES
    "${dosbox_core_SOURCE_DIR}/src/*_win32.cpp"
    "${dosbox_core_SOURCE_DIR}/src/*_os2.cpp"
    "${dosbox_core_SOURCE_DIR}/src/libs/gui_tk/*"
    "${dosbox_core_SOURCE_DIR}/src/dos/cdrom.cpp"
    "${dosbox_core_SOURCE_DIR}/src/dos/cdrom_ioctl_*.cpp"
    "${dosbox_core_SOURCE_DIR}/src/dos/cdrom_aspi_*.cpp"
    "${dosbox_core_SOURCE_DIR}/src/libs/zmbv/drvproc.cpp"
    "${dosbox_core_SOURCE_DIR}/src/libs/zmbv/zmbv_vfw.cpp"
    "${dosbox_core_SOURCE_DIR}/src/gui/sdl*.cpp"
)
if(EXCLUDE_SOURCES)
    list(REMOVE_ITEM DOSBOX_SOURCES ${EXCLUDE_SOURCES})
endif()

add_library(dosbox_core_lib STATIC ${DOSBOX_SOURCES})

# Explicitly add include directories to the library
target_include_directories(dosbox_core_lib PUBLIC
    "${dosbox_core_SOURCE_DIR}/include"
    "${dosbox_core_SOURCE_DIR}/src"
)

# Map the standard main to the expected entry point name
target_compile_definitions(dosbox_core_lib PRIVATE
    -Dmain=dosbox_main_entry
    -DANDROID
    -DC_HAS_ATTRIBUTE_ALWAYS_INLINE
)

# Disable errors for deprecated 'register' keyword (removed in C++17)
target_compile_options(dosbox_core_lib PRIVATE -Wno-register)

# -----------------------------------------------------------------------------
# Define Shared JNI Emulator Library
# -----------------------------------------------------------------------------
add_library(retrorts_dosbox_jni SHARED
    dosbox_jni.cpp
    dosbox_audio.cpp
    emulator_core.cpp
    dsi_core.cpp
    native-lib.cpp
    sdl_stub.cpp
)

# Ensure all headers are visible
target_include_directories(retrorts_dosbox_jni PRIVATE
    "${dosbox_core_SOURCE_DIR}/include"
    "${dosbox_core_SOURCE_DIR}/src"
)

find_library(log-lib log)
find_library(android-lib android)
find_library(aaudio-lib aaudio)

target_link_libraries(retrorts_dosbox_jni
    dosbox_core_lib
    ${log-lib}
    ${android-lib}
    ${aaudio-lib}
)
