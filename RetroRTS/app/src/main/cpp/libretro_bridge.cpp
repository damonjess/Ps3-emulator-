#include "libretro_bridge.h"
#include <dlfcn.h>
#include <android/log.h>
#include <thread>
#include <chrono>
#include <android/native_window_jni.h>
#include <algorithm>

#define LOG_TAG "LibretroBridge"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

namespace retrorts {

LibretroHost& LibretroHost::getInstance() {
    static LibretroHost instance;
    return instance;
}

LibretroHost::LibretroHost() {}

LibretroHost::~LibretroHost() {
    stop();
}

int LibretroHost::loadCore(const std::string& corePath) {
    std::lock_guard<std::mutex> lock(coreMutex_);
    if (coreLib_) {
        dlclose(coreLib_);
        coreLib_ = nullptr;
    }

    LOGI("Loading core: %s", corePath.c_str());
    coreLib_ = dlopen(corePath.c_str(), RTLD_NOW);
    if (!coreLib_) {
        LOGE("Failed to load core %s: %s", corePath.c_str(), dlerror());
        return -1;
    }

    retro_init_fn = (void (*)())dlsym(coreLib_, "retro_init");
    retro_deinit_fn = (void (*)())dlsym(coreLib_, "retro_deinit");
    retro_run_fn = (void (*)())dlsym(coreLib_, "retro_run");
    retro_load_game_fn = (bool (*)(const struct retro_game_info*))dlsym(coreLib_, "retro_load_game");
    retro_unload_game_fn = (void (*)())dlsym(coreLib_, "retro_unload_game");
    retro_get_system_av_info_fn = (void (*)(struct retro_system_av_info*))dlsym(coreLib_, "retro_get_system_av_info");
    retro_set_environment_fn = (void (*)(retro_environment_t))dlsym(coreLib_, "retro_set_environment");
    retro_set_video_refresh_fn = (void (*)(retro_video_refresh_t))dlsym(coreLib_, "retro_set_video_refresh");
    retro_set_audio_sample_fn = (void (*)(retro_audio_sample_t))dlsym(coreLib_, "retro_set_audio_sample");
    retro_set_audio_sample_batch_fn = (void (*)(retro_audio_sample_batch_t))dlsym(coreLib_, "retro_set_audio_sample_batch");
    retro_set_input_poll_fn = (void (*)(retro_input_poll_t))dlsym(coreLib_, "retro_set_input_poll");
    retro_set_input_state_fn = (void (*)(retro_input_state_t))dlsym(coreLib_, "retro_set_input_state");

    if (!retro_init_fn || !retro_run_fn || !retro_load_game_fn) {
        LOGE("Core is missing essential symbols");
        dlclose(coreLib_);
        coreLib_ = nullptr;
        return -2;
    }

    retro_set_environment_fn(envCallback);
    retro_set_video_refresh_fn(videoCallback);
    retro_set_audio_sample_fn(audioCallback);
    retro_set_audio_sample_batch_fn(audioBatchCallback);
    retro_set_input_poll_fn(inputPollCallback);
    retro_set_input_state_fn(inputStateCallback);

    retro_init_fn();
    LOGI("Core initialized");
    return 0;
}

int LibretroHost::loadGame(const std::string& romPath) {
    std::lock_guard<std::mutex> lock(coreMutex_);
    if (!coreLib_ || !retro_load_game_fn) return -1;

    struct retro_game_info game = {romPath.c_str(), nullptr, 0, nullptr};
    if (!retro_load_game_fn(&game)) {
        LOGE("Failed to load game: %s", romPath.c_str());
        return -2;
    }

    running_.store(true);
    LOGI("Game loaded: %s", romPath.c_str());
    return 0;
}

void LibretroHost::runLoop() {
    while (running_.load()) {
        {
            std::lock_guard<std::mutex> lock(coreMutex_);
            if (retro_run_fn) retro_run_fn();
        }
        // Basic throttle if the core doesn't handle it
        std::this_thread::sleep_for(std::chrono::milliseconds(1));
    }
}

void LibretroHost::setWindow(ANativeWindow* window) {
    std::lock_guard<std::mutex> lock(coreMutex_);
    if (window_) ANativeWindow_release(window_);
    window_ = window;
    if (window_) {
        ANativeWindow_acquire(window_);
        ANativeWindow_setBuffersGeometry(window_, 0, 0, WINDOW_FORMAT_RGBX_8888);
    }
}

void LibretroHost::stop() {
    running_.store(false);
    std::lock_guard<std::mutex> lock(coreMutex_);
    if (window_) {
        ANativeWindow_release(window_);
        window_ = nullptr;
    }
    if (coreLib_) {
        if (retro_unload_game_fn) retro_unload_game_fn();
        if (retro_deinit_fn) retro_deinit_fn();
        dlclose(coreLib_);
        coreLib_ = nullptr;
    }
}

bool LibretroHost::envCallback(unsigned cmd, void* data) {
    switch (cmd) {
        case RETRO_ENVIRONMENT_GET_CAN_DUPE:
            *(bool*)data = true;
            return true;
        case RETRO_ENVIRONMENT_SET_PIXEL_FORMAT:
            // Handle pixel format if needed
            return true;
        default:
            return false;
    }
}

void LibretroHost::videoCallback(const void* data, unsigned width, unsigned height, size_t pitch) {
    if (!data) return;
    auto& host = getInstance();
    std::lock_guard<std::mutex> lock(host.coreMutex_);
    if (!host.window_) return;

    ANativeWindow_Buffer buffer;
    if (ANativeWindow_lock(host.window_, &buffer, nullptr) != 0) return;

    uint32_t* dst = static_cast<uint32_t*>(buffer.bits);
    const uint16_t* src = static_cast<const uint16_t*>(data); // Assuming RGB565 for most cores initially

    int copy_w = std::min((int)width, buffer.width);
    int copy_h = std::min((int)height, buffer.height);

    for (int y = 0; y < copy_h; y++) {
        uint32_t* dst_row = dst + y * buffer.stride;
        const uint16_t* src_row = src + y * (pitch / 2);
        for (int x = 0; x < copy_w; x++) {
            uint16_t px = src_row[x];
            uint8_t r = (px >> 11) << 3;
            uint8_t g = ((px >> 5) & 0x3f) << 2;
            uint8_t b = (px & 0x1f) << 3;
            dst_row[x] = (0xff << 24) | (b << 16) | (g << 8) | r;
        }
    }

    ANativeWindow_unlockAndPost(host.window_);
}

void LibretroHost::audioCallback(int16_t left, int16_t right) {
    int16_t samples[2] = {left, right};
    audioBatchCallback(samples, 1);
}

size_t LibretroHost::audioBatchCallback(const int16_t* data, size_t frames) {
    // Audio submission logic will go here
    return frames;
}

void LibretroHost::inputPollCallback() {
    // Poll input from Android layer
}

int16_t LibretroHost::inputStateCallback(unsigned port, unsigned device, unsigned index, unsigned id) {
    // Return input state
    return 0;
}

// Legacy Bridge Implementation
extern "C" int PCSX_Run(const char* bios, const char* disc, const char* saveDir) {
    LOGI("Bridge: PCSX_Run called for %s", disc);
    auto& host = LibretroHost::getInstance();
    if (host.loadCore("libpcsx_rearmed_libretro.so") != 0) {
        // Fallback to non-libretro names if needed, or error out
        if (host.loadCore("libpcsx_rearmed.so") != 0) return -1;
    }
    if (host.loadGame(disc) != 0) return -2;

    std::thread([&host]() { host.runLoop(); }).detach();
    return 0;
}

extern "C" int uae_init(const char* config_path) {
    LOGI("Bridge: uae_init called for %s", config_path);
    auto& host = LibretroHost::getInstance();
    if (host.loadCore("libpuae_libretro.so") != 0) {
        if (host.loadCore("libpuae.so") != 0) return -1;
    }
    // UAE core often takes the .uae config as the "game"
    if (host.loadGame(config_path) != 0) return -2;

    std::thread([&host]() { host.runLoop(); }).detach();
    return 0;
}

extern "C" int dosbox_init(const char* config_path, const char* saveDir) {
    LOGI("Bridge: dosbox_init called for %s", config_path);
    auto& host = LibretroHost::getInstance();
    if (host.loadCore("libdosbox_pure_libretro.so") != 0) {
        if (host.loadCore("libdosbox_pure.so") != 0) return -1;
    }
    if (host.loadGame(config_path) != 0) return -2;

    std::thread([&host]() { host.runLoop(); }).detach();
    return 0;
}

} // namespace retrorts
