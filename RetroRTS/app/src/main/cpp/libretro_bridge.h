#pragma once

#include <string>
#include <atomic>
#include <mutex>
#include <android/native_window.h>
#include "libretro.h"

namespace retrorts {

class LibretroHost {
public:
    static LibretroHost& getInstance();

    int loadCore(const std::string& corePath);
    int loadGame(const std::string& romPath);
    void runLoop();
    void stop();

    void setWindow(ANativeWindow* window);
    bool isRunning() const { return running_; }

    // Libretro callbacks
    static bool envCallback(unsigned cmd, void* data);
    static void videoCallback(const void* data, unsigned width, unsigned height, size_t pitch);
    static void audioCallback(int16_t left, int16_t right);
    static size_t audioBatchCallback(const int16_t* data, size_t frames);
    static void inputPollCallback();
    static int16_t inputStateCallback(unsigned port, unsigned device, unsigned index, unsigned id);

private:
    LibretroHost();
    ~LibretroHost();

    void* coreLib_ = nullptr;
    ANativeWindow* window_ = nullptr;
    std::atomic<bool> running_{false};
    std::mutex coreMutex_;

    // Core functions
    void (*retro_init_fn)() = nullptr;
    void (*retro_deinit_fn)() = nullptr;
    void (*retro_run_fn)() = nullptr;
    bool (*retro_load_game_fn)(const struct retro_game_info*) = nullptr;
    void (*retro_unload_game_fn)() = nullptr;
    void (*retro_get_system_av_info_fn)(struct retro_system_av_info*) = nullptr;
    void (*retro_set_environment_fn)(retro_environment_t) = nullptr;
    void (*retro_set_video_refresh_fn)(retro_video_refresh_t) = nullptr;
    void (*retro_set_audio_sample_fn)(retro_audio_sample_t) = nullptr;
    void (*retro_set_audio_sample_batch_fn)(retro_audio_sample_batch_t) = nullptr;
    void (*retro_set_input_poll_fn)(retro_input_poll_t) = nullptr;
    void (*retro_set_input_state_fn)(retro_input_state_t) = nullptr;
};

// Legacy bridge exports
extern "C" {
    int PCSX_Run(const char* bios, const char* disc, const char* saveDir);
    int uae_init(const char* config_path);
    int dosbox_init(const char* config_path, const char* saveDir);
}

} // namespace retrorts
