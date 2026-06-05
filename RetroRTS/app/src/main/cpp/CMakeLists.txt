cmake_minimum_required(VERSION 3.22.1)
project(pcsx_rearmed LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)
FetchContent_Declare(
    pcsx_rearmed
    GIT_REPOSITORY https://github.com/notaz/pcsx_rearmed.git
    GIT_TAG        master
    GIT_SHALLOW    TRUE
)
FetchContent_MakeAvailable(pcsx_rearmed)

# Build PCSX core as a STATIC library
add_library(pcsx_core STATIC
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/psxbios.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/psxcounters.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/psxdma.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/psxhw.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/psxinterpreter.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/psxmem.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/r3000a.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/cdrom.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/cdriso.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/cheat.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/decode_xa.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/mdec.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/misc.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/sio.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/ppf.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/psxcommon.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/psxevents.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/plugins.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/database.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/gpu.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/gte.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/spu.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/cdrom-async.c
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore/gte_divider.c
)

target_include_directories(pcsx_core PUBLIC
    ${pcsx_rearmed_SOURCE_DIR}
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore
    ${pcsx_rearmed_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}
)

target_compile_definitions(pcsx_core PRIVATE
    __ANDROID__=1
    NEON_BUILD=1
    HAVE_ZLIB=1
    DRC_DISABLE=1
    BUILT_IN_GPU=1
)

# Link zlib
find_package(ZLIB REQUIRED)
target_link_libraries(pcsx_core PRIVATE ZLIB::ZLIB)

# Our main JNI shared library (renamed to pcsx_rearmed as requested)
add_library(pcsx_rearmed SHARED
    native-lib.cpp
    emulator_core.cpp
    dsi_core.cpp
    ps1_core.cpp
    amiga_core.cpp
    dosbox_bridge_jni.cpp
    dosbox_audio.cpp
    surface_jni.cpp
    pcsx_jni_entry.cpp
    gpu_android.cpp
)

find_library(log-lib log)
find_library(android-lib android)
find_library(aaudio-lib aaudio)

target_link_libraries(pcsx_rearmed
    pcsx_core
    ${log-lib}
    ${android-lib}
    ${aaudio-lib}
)

target_include_directories(pcsx_rearmed PRIVATE
    ${pcsx_rearmed_SOURCE_DIR}
    ${pcsx_rearmed_SOURCE_DIR}/libpcsxcore
    ${pcsx_rearmed_SOURCE_DIR}/include
)
