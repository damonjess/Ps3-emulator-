cmake_minimum_required(VERSION 3.22.1)
project(retrorts_dosbox LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(NOT ANDROID_ABI STREQUAL "arm64-v8a")
    message(FATAL_ERROR "This DOSBox build is configured for arm64-v8a only. Current ABI: ${ANDROID_ABI}")
endif()

set(DOSBOX_SRC_DIR ${CMAKE_CURRENT_SOURCE_DIR}/dosbox)
set(DOSBOX_SOURCES
    ${DOSBOX_SRC_DIR}/src/dos/dos.cpp
    ${DOSBOX_SRC_DIR}/src/dos/dos_programs.cpp
    ${DOSBOX_SRC_DIR}/src/cpu/cpu.cpp
    ${DOSBOX_SRC_DIR}/src/cpu/core_dyn_x86.cpp
    ${DOSBOX_SRC_DIR}/src/hardware/timer.cpp
    ${DOSBOX_SRC_DIR}/src/hardware/pic.cpp
    ${DOSBOX_SRC_DIR}/src/hardware/mixer.cpp
    ${DOSBOX_SRC_DIR}/src/gui/sdlmain.cpp
    ${DOSBOX_SRC_DIR}/src/ints/int10.cpp
    ${DOSBOX_SRC_DIR}/src/ints/int13.cpp
)

add_library(dosbox_core STATIC ${DOSBOX_SOURCES})
target_include_directories(dosbox_core PUBLIC ${DOSBOX_SRC_DIR}/include ${DOSBOX_SRC_DIR}/src)
target_compile_definitions(dosbox_core PUBLIC C_SDL2=1 ANDROID=1)

add_library(retrorts_dosbox_jni SHARED
    dosbox_jni.cpp
    dosbox_audio.cpp
)

find_library(log-lib log)
find_library(android-lib android)
find_library(aaudio-lib aaudio)

# If oboe is vendored (recommended), include and link it.
# add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/oboe)
# target_link_libraries(retrorts_dosbox_jni oboe::oboe)

target_link_libraries(retrorts_dosbox_jni
    dosbox_core
    ${log-lib}
    ${android-lib}
    ${aaudio-lib}
)
