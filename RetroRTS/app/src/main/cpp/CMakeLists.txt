cmake_minimum_required(VERSION 3.22.1)
project(retrorts_dosbox LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

if(NOT ANDROID_ABI STREQUAL "arm64-v8a")
    message(FATAL_ERROR "This build is configured for arm64-v8a only. Current ABI: ${ANDROID_ABI}")
endif()

add_library(retrorts_dosbox_jni SHARED
    dosbox_jni.cpp
    dosbox_audio.cpp
    emulator_core.cpp
    native-lib.cpp
)

find_library(log-lib log)
find_library(android-lib android)
find_library(aaudio-lib aaudio)

target_link_libraries(retrorts_dosbox_jni
    ${log-lib}
    ${android-lib}
    ${aaudio-lib}
)
