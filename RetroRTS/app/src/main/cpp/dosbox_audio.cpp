#include "dosbox_audio.h"
#include <algorithm>

bool DosboxAudioEngine::start(int, int) {
    std::lock_guard<std::mutex> lock(mutex_);
    running_ = true;
    ringBuffer_.clear();
    return true;
}

void DosboxAudioEngine::stop() {
    std::lock_guard<std::mutex> lock(mutex_);
    running_ = false;
    ringBuffer_.clear();
}

void DosboxAudioEngine::setVolume(float volume) {
    std::lock_guard<std::mutex> lock(mutex_);
    volume_ = std::clamp(volume, 0.0f, 1.0f);
}

void DosboxAudioEngine::submitPcm16(const int16_t* data, int frames, int channels) {
    std::lock_guard<std::mutex> lock(mutex_);
    if (!running_ || data == nullptr || frames <= 0 || channels <= 0) return;

    const int samples = frames * channels;
    ringBuffer_.reserve(ringBuffer_.size() + samples);
    for (int i = 0; i < samples; ++i) {
        float scaled = static_cast<float>(data[i]) * volume_;
        int sample = static_cast<int>(scaled);
        sample = std::clamp(sample, -32768, 32767);
        ringBuffer_.push_back(static_cast<int16_t>(sample));
    }

    if (ringBuffer_.size() > kMaxBufferedSamples) {
        const size_t trim = ringBuffer_.size() - kMaxBufferedSamples;
        ringBuffer_.erase(ringBuffer_.begin(), ringBuffer_.begin() + static_cast<long>(trim));
    }
}


