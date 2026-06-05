#pragma once
#include <android/native_window.h>
#include <cstdint>

#ifdef __cplusplus
extern "C" {
#endif

void gpu_android_set_window(ANativeWindow* window);
void gpu_android_set_vram(uint16_t* vram);
void GPU_vBlank_android(int val, int start);
void GPU_writeStatus_android(uint32_t data);
void GPU_updateLace(void);

#ifdef __cplusplus
}
#endif
