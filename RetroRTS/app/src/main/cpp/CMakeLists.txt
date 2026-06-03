cmake_minimum_required(VERSION 3.22.1)
project(retrorts_jni LANGUAGES C CXX)
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
if(NOT ANDROID_ABI STREQUAL "arm64-v8a")
message(FATAL_ERROR "arm64-v8a only. Got: ${ANDROID_ABI}")
endif()
add_library(retrorts_jni SHARED
native-lib.cpp
emulator_core.cpp
dsi_core.cpp
ps1_core.cpp
dosbox_bridge_jni.cpp
dosbox_audio.cpp
surface_jni.cpp
)
find_library(log-lib log)
find_library(android-lib android)
find_library(aaudio-lib aaudio)
target_link_libraries(retrorts_jni
${log-lib}
${android-lib}
${aaudio-lib}
)
