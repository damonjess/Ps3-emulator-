#pragma once

#include <cstdint>
#include <vector>
#include <mutex>

class DosboxAudioEngine {
public:
    bool start(int sampleRate, int channels);
    void stop();
    void setVolume(float volume);
    void submitPcm16(const int16_t* data, int frames, int channels);

private:
    static constexpr size_t kMaxBufferedSamples = 48000 * 2 * 2; // ~2s stereo @48k
    std::mutex mutex_;
    std::vector<int16_t> ringBuffer_;
    float volume_ = 1.0f;
    bool running_ = false;
};
